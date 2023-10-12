package furhatos.app.customasr.aws

import aws.sdk.kotlin.services.sagemakerruntime.SageMakerRuntimeClient
import aws.sdk.kotlin.services.sagemakerruntime.model.InvokeEndpointRequest
import com.google.gson.Gson
import furhatos.app.customasr.ListenDone
import furhatos.app.customasr.ListenStarted
import furhatos.app.customasr.com.FurhatAudioFeedStreamer
import furhatos.app.customasr.com.FurhatAudioStream
import furhatos.demo.audiofeed.FurhatAudioFeedRecorder
import furhatos.demo.utils.WavFileWriter
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

/**
 * Handles the events returned by AWS transcribe. Mostly sends events back in the system.
 */
//fun getTranscriptor(): StartStreamTranscriptionResponseHandler {
//    return StartStreamTranscriptionResponseHandler.builder()
//        .onResponse { _: StartStreamTranscriptionResponse ->
//            EventSystem.send(ListenStarted())
//            logger.info("=== Received Initial response ===")
//        }
//        .onError { e: Throwable ->
//            logger.warn(e.message)
//            val sw = StringWriter()
//            e.printStackTrace(PrintWriter(sw))
//            logger.warn("Error Occurred: $sw")
//            EventSystem.send(ListenDone())
//        }
//        .onComplete {
//            EventSystem.send(ListenDone())
//            logger.info("=== All records stream successfully ===")
//        }
//        .subscriber { event: TranscriptResultStream ->
//            val results = (event as TranscriptEvent).transcript().results()
//            if (results.size > 0) {
//                if (results[0].alternatives().size > 0) {
//                    if (results[0].alternatives()[0].transcript().isNotEmpty()) {
//                        val result = results[0]
//                        val message = result.alternatives()[0].transcript()
//                        EventSystem.send(InterimResult(message, result.isPartial))
//                    }
//                }
//            }
//        }
//        .build()
//}

fun audioStreamToEvent(
        lang: Language, recorder: FurhatAudioFeedRecorder,
        timeout: Long, endSil: Long, maxSpeech: Long
) {
    // We essentially have two modes: waiting for speech or waiting for silence

    // Waiting for speech
    val initialSecondsSinceLastSpeech = recorder.getSecondsSinceLastSpeech() + 0.5 // 500ms buffer in case user is super fast
    var secondsSinceLastSpeech = recorder.getSecondsSinceLastSpeech()

    while(secondsSinceLastSpeech >= initialSecondsSinceLastSpeech) {
        // no speech yet, wait for 200ms
        Thread.sleep(200)
        if (secondsSinceLastSpeech > timeout) {
            // timeout reached, stop listening
            EventSystem.send(ListenDone(""))
            return
        }
        secondsSinceLastSpeech = recorder.getSecondsSinceLastSpeech()
    }

    // listen was requested, we have some time before the user finishes speaking
    var audioByteArray: ByteArray = byteArrayOf()
    Thread.sleep(1000)




    while (audioByteArray.isEmpty()) {
        Thread.sleep(200)
        audioByteArray = recorder.getLast20Seconds()
    }
    // write wav file to disk using wavfilewriter
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
    SageMakerRuntimeClient { region = "eu-north-1" }.use { sageMakerRuntimeClient ->
        val response = runBlocking {
            sageMakerRuntimeClient.invokeEndpoint(invokeEndpointRequest)
        }
        // decode Map from byteArray
        val map = Gson().fromJson(response.body?.toString(charset = Charsets.UTF_8), Map::class.java)
        val text = map["text"].toString()
        EventSystem.send(ListenDone(text))
    }
}