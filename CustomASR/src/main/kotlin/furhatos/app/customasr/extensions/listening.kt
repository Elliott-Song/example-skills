package furhatos.app.customasr.extensions

import furhatos.app.customasr.*
import furhatos.event.EventSystem
import furhatos.flow.kotlin.*
import furhatos.app.customasr.audiofeed.FurhatAudioFeedStreamer
import furhatos.app.customasr.flow.customListening
import furhatos.demo.audiofeed.FurhatAudioFeedRecorder
import furhatos.event.Event
import furhatos.nlu.common.*
import furhatos.util.CommonUtils

private val recorder = FurhatAudioFeedRecorder()

// Attemps to forcefully enable the audio feed on the Furhat and starts receiving the published stream.
fun Furhat.enableStartAudioStream() {
    this.audioFeed.enable()
    FurhatAudioFeedStreamer.start(params.ROBOT_IP_ADDRESS)
}

fun Furhat.customAsk(text: String) {
    this.say(text)
    this.customListen()
}
fun Furhat.customListen(
        timeout: Long = params.timeout,
        endSil: Long = params.endSil,
        maxSpeech: Long = params.maxSpeech,
) {
    recorder.startRecording()
    EventSystem.send(ListenStarted(timeout, endSil, maxSpeech))
}

// Forwards NoSpeechDetected to the standard UserSilence
fun StateBuilder.onUserSilence(trigger: TriggerRunner<*>.(NoSpeechDetected) -> Unit) {
    onEvent<NoSpeechDetected> {
        trigger.invoke(this, it)
    }
}

// Handles listening on a separate thread to the main flow
val ParallelListenState = state {
    onEvent<ListenStarted>(instant = true) {
        call(waitForTranscription(it.timeout, it.endSil, it.maxSpeech))
    }
}
fun waitForTranscription(timeout: Long, endSil: Long, maxSpeech: Long) = state {
    var speechWasRecognized = false
    var lastSpeechTime = -1L
    val logger = CommonUtils.getLogger("ListenState")
    var resultingEvent: Event = NoSpeechDetected()

    onEvent<InterimResult>(instant = true) { // Got recognized speech
        speechWasRecognized = true
        lastSpeechTime = System.currentTimeMillis()
    }

    /**
     * Checks how long ago the last speech was recognized, ends the stream if its longer than the endSil param.
     */
    onTime(repeat = 100, instant = true, cond = { speechWasRecognized }) {
        if(
                lastSpeechTime != -1L &&
                System.currentTimeMillis() - lastSpeechTime > endSil
        ) {
            speechWasRecognized = false
            logger.info("endSil for listen reached.")
            recorder.stopRecording()
            Thread.sleep(400)
            resultingEvent = customListenDone(recorder.getLatestTranscription())
            terminate()
        }
    }

    /**
     * If it takes longer than initialTimeout for speech to be recognized, end the stream.
     */
    onTime(delay = timeout.toInt(), instant = true, cond = { !speechWasRecognized} ) {
        logger.info("timeout for listen reached.")
        recorder.stopRecording()
        terminate()
    }

    /**
     * End the stream if maxSpeech timeout is reached.
     */
    onTime(delay = maxSpeech.toInt(), instant = true) {
        logger.info("maxSpeech for listen reached.")
        recorder.stopRecording()
        resultingEvent = customListenDone(recorder.getLatestTranscription())
        terminate()
    }

    onExit {
        EventSystem.send(resultingEvent)
        logger.info("Exiting listen state from ${getAllRunners()}")
    }
}

// Ask yes or no functionality
fun Furhat.customAskYN(text: String): Boolean {
    customAsk(text)
    return runner.call(askYNState()) as Boolean
}

fun askYNState() = state {
    include(customListening)
    onResponse<Yes> {
        terminate(true)
    }
    onResponse<No> {
        terminate(false)
    }
    onResponse<Maybe> {
        furhat.customAsk("Pick yes or no.")
    }
    onResponse<DontKnow> {
        furhat.customAsk("Please say either yes or no.")
    }
    onResponse {
        terminate(false)
    }
}