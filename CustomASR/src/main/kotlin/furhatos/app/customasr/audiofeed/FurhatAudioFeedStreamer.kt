package furhatos.app.customasr.audiofeed

import org.zeromq.SocketType
import org.zeromq.ZMQ
import javax.sound.sampled.AudioFormat
import kotlin.concurrent.thread

/**
 * Sends data from the audioFeed to all registered listeners.
 * (Make sure to enable the Audio Feed under "External Feeds" in the web console.)
 */
object FurhatAudioFeedStreamer {
    val context: ZMQ.Context = ZMQ.context(1)
    var running = false
        private set
    var runThread: Thread? = null
    val audioFormat = AudioFormat(16000F, 16, 2, true, false)
    val audioListeners = mutableListOf<AudioStreamingListener>()
    var ipaddr = ""

    interface AudioStreamingListener {

        fun stopRecording()
        fun audioStreamingStarted()
        fun audioStreamingData(data: ByteArray)
    }

    fun addListener(audioListener: AudioStreamingListener) {
        audioListeners += audioListener
    }

    fun start(ipaddr: String) {
        if (ipaddr != FurhatAudioFeedStreamer.ipaddr) {
            stop()
            FurhatAudioFeedStreamer.ipaddr = ipaddr
        } else if (running) {
            return
        }
        val socket = context.socket(SocketType.SUB).apply {
            receiveTimeOut = 1000
            subscribe(byteArrayOf())
            connect("tcp://$ipaddr:3001")
        }
        running = true
        audioListeners.forEach { it.audioStreamingStarted() }
        runThread = thread(start = true) {
            while (running) {
                val data = socket!!.recv()
                if (data != null && data.isNotEmpty()) {
                    audioListeners.forEach { it.audioStreamingData(data) }
                }
            }
        }
    }

    fun stop() {
        if (running) {
            running = false
            runThread?.join()
            runThread = null
            audioListeners.forEach { it.stopRecording() }
        }
    }
}
