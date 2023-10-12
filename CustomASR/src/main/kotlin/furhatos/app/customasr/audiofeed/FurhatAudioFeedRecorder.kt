package furhatos.demo.audiofeed

import furhatos.app.customasr.com.FurhatAudioFeedStreamer
import furhatos.demo.utils.WavFileWriter
import furhatos.demo.utils.removeRightChannel
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs

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
                if (shorttermAverageAmp - longtermAverageAmp > 100) {
                    secondsSinceLastSpeech = 0.0
//                    println("Louder than usual! short: $shorttermAverageAmp, long: $longtermAverageAmp")
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

    fun getMillisSinceLastSpeech(): Int {
        return (secondsSinceLastSpeech * 1000).toInt()
    }

    fun getLast20Seconds(): ByteArray {
        stop()
        // write audio data to output stream in correct order
        val outputStream = ByteArrayOutputStream()
        outputStream.write(circularBuffer, bufferIndex, maxAudioDataSize - bufferIndex)
        outputStream.write(circularBuffer, 0, bufferIndex)
        startRecording()

        return outputStream.toByteArray()
    }

    override fun audioStreamingStopped() {
        stop()
    }

    override fun audioStreamingStarted() {}
}