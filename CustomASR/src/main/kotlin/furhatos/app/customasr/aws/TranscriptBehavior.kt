package furhatos.app.customasr.aws

import aws.sdk.kotlin.services.sagemakerruntime.SageMakerRuntimeClient
import aws.sdk.kotlin.services.sagemakerruntime.model.InvokeEndpointRequest
import com.google.gson.Gson
import furhatos.app.customasr.ListenDone
import furhatos.app.customasr.com.FurhatAudioFeedStreamer
import furhatos.demo.audiofeed.FurhatAudioFeedRecorder
import furhatos.util.Language
import furhatos.event.EventSystem
import furhatos.util.CommonUtils
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

private val logger = CommonUtils.getLogger("TranscriptResponseHandler")

fun audioStreamToEvent(
        lang: Language, recorder: FurhatAudioFeedRecorder,
        timeout: Long, endSil: Long, maxSpeech: Long
) {
    // We essentially have two steps: waiting for speech, and then waiting for silence
    val startTime = System.currentTimeMillis()

    // Waiting for speech
    var millisSinceLastSpeech = recorder.getMillisSinceLastSpeech()
    while(millisSinceLastSpeech >= 500) { // 500ms buffer in case user is super fast
        // no speech yet, wait for 200ms
        Thread.sleep(200)
        if (millisSinceLastSpeech > timeout) {
            // timeout reached, stop listening
            EventSystem.send(ListenDone(""))
            return
        }
        millisSinceLastSpeech = recorder.getMillisSinceLastSpeech()
    }

    // Waiting for silence
    while(millisSinceLastSpeech < endSil && System.currentTimeMillis() - startTime < maxSpeech) {
        // no prolonged silence yet, wait for 200ms
        Thread.sleep(200)
        millisSinceLastSpeech = recorder.getMillisSinceLastSpeech()
    }
    val audioByteArray: ByteArray = recorder.getLast20Seconds()

    // write wav file to disk for debugging
//    val wavFileWriter = WavFileWriter()
//    wavFileWriter.open(File("audio.wav"), FurhatAudioFeedStreamer.audioFormat)
//    wavFileWriter.writeAudio(audioByteArray, 0, audioByteArray.size)
//    wavFileWriter.close()

    val inStream = audioByteArray.inputStream()
    val outStream = ByteArrayOutputStream()
    val ai = AudioInputStream(inStream, FurhatAudioFeedStreamer.audioFormat, audioByteArray.size.toLong())
    AudioSystem.write(ai, AudioFileFormat.Type.WAVE, outStream)
    val wavByteArray = outStream.toByteArray()
    // write wav file to disk for debugging
    val wavFile = File("audio_processed.wav")
    wavFile.writeBytes(wavByteArray)

    val invokeEndpointRequest = InvokeEndpointRequest {
        endpointName = "jumpstart-dft-hf-asr-whisper-medium"
        contentType = "audio/wav"
        body = wavByteArray
    }
    SageMakerRuntimeClient {
        region = "eu-north-1"

    }.use { sageMakerRuntimeClient ->
        val response = runBlocking {
            sageMakerRuntimeClient.invokeEndpoint(invokeEndpointRequest)
        }
        // decode Map from byteArray
        val map = Gson().fromJson(response.body?.toString(charset = Charsets.UTF_8), Map::class.java)
        val text = map["text"].toString()
        EventSystem.send(ListenDone(text))
    }
}