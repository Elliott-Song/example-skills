package furhatos.demo.audiofeed

import aws.sdk.kotlin.services.sagemakerruntime.SageMakerRuntimeClient
import aws.sdk.kotlin.services.sagemakerruntime.model.InvokeEndpointRequest
import com.google.gson.Gson
import furhatos.app.customasr.InterimResult
import furhatos.app.customasr.audiofeed.FurhatAudioFeedStreamer
import furhatos.demo.utils.removeRightChannel
import furhatos.event.EventSystem
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.min

/**
 * Records audio from the audioFeed and sends it to SageMaker for transcription.
 */
class FurhatAudioFeedRecorder(): FurhatAudioFeedStreamer.AudioStreamingListener {
    private var recordUser: Boolean = false

    var longtermAverageAmp = 0
    var shorttermAverageAmp = 0
    var secondsSinceLastSpeech = 0.0
    val sampleRate = FurhatAudioFeedStreamer.audioFormat.sampleRate
    val bytesPerSample = FurhatAudioFeedStreamer.audioFormat.sampleSizeInBits / 8
    val maxAudioDataSize = (10 * sampleRate * bytesPerSample).toInt() // 20 seconds of audio
    var circularBuffer = ByteArray(maxAudioDataSize)
    var bufferIndex = 0

    var running = false
        private set

    init {
        FurhatAudioFeedStreamer.addListener(this)
    }

    fun startRecording() {
        if (running)
            stop()
        running = true
        recordUser = true
        circularBuffer = ByteArray(maxAudioDataSize)
        secondsSinceLastSpeech = min(secondsSinceLastSpeech, 1.0)
    }

    private fun stop() {
        if (!running)
            return
        running = false
        recordUser = false
    }

    override fun audioStreamingData(data: ByteArray) {
        if (running) {
            // Write user only (audio in) when asked (function startRecordSeparate())
            if (recordUser) {
                val audioInData = data.copyOf()
                removeRightChannel(audioInData)

                // get average amplitude of audioInData
                val numSamples = audioInData.size / 2
                var totalAmplitude = 0
                for (i in 0 until numSamples) {
                    val sample = (audioInData[2 * i + 1].toInt() shl 8) + audioInData[2 * i].toInt()
                    totalAmplitude += abs(sample)
                }
                val averageAmp = totalAmplitude / numSamples

                // calculate short and long term average amplitude
                shorttermAverageAmp = (shorttermAverageAmp * 0.7 + averageAmp * 0.3).toInt()
                longtermAverageAmp = (longtermAverageAmp * 0.95 + averageAmp * 0.05).toInt()

                // check if it is louder than usual to detect speech
                if (shorttermAverageAmp - longtermAverageAmp > 200 && secondsSinceLastSpeech > 0.5) {
                    EventSystem.send(InterimResult())
                    secondsSinceLastSpeech = 0.0
                    println("Louder than usual! short: $shorttermAverageAmp, long: $longtermAverageAmp")
                } else {
                    secondsSinceLastSpeech += numSamples / sampleRate
                }

                val dataLength = audioInData.size
                val bytesForEnd = (maxAudioDataSize - bufferIndex).coerceAtMost(dataLength)
                System.arraycopy(audioInData, 0, circularBuffer, bufferIndex, bytesForEnd)
                bufferIndex = (bufferIndex + bytesForEnd) % maxAudioDataSize
            }
        }
    }

    fun getLast20Seconds(): ByteArray {
        // write audio data to output stream in correct order
        val outputStream = ByteArrayOutputStream()
        outputStream.write(circularBuffer, bufferIndex, maxAudioDataSize - bufferIndex)
        outputStream.write(circularBuffer, 0, bufferIndex)
        return outputStream.toByteArray()
    }

    fun getTranscription(): String {
        val last20Seconds = getLast20Seconds()
        val inStream = last20Seconds.inputStream()
        val outStream = ByteArrayOutputStream()
        val ai = AudioInputStream(inStream, FurhatAudioFeedStreamer.audioFormat, last20Seconds.size.toLong())
        AudioSystem.write(ai, AudioFileFormat.Type.WAVE, outStream)
        val wavByteArray = outStream.toByteArray()
        // write wav file to disk for debugging
//        val wavFile = File("audio_processed.wav")
//        wavFile.writeBytes(wavByteArray)

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
            println("Transcribed: $text")
            return text
        }
    }

    override fun stopRecording() {
        stop()
    }

    override fun audioStreamingStarted() {}
}