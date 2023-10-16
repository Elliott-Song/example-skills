package furhatos.app.customasr.extensions

import furhatos.app.customasr.ListenStarted
import furhatos.app.customasr.audiofeed.audioStreamToEvent
import furhatos.app.customasr.params
import furhatos.app.customasr.nlu.NoSpeechDetected
import furhatos.event.EventSystem
import furhatos.flow.kotlin.*
import furhatos.app.customasr.audiofeed.FurhatAudioFeedStreamer
import furhatos.demo.audiofeed.FurhatAudioFeedRecorder
import furhatos.util.CommonUtils

private val recorder = FurhatAudioFeedRecorder()

fun Furhat.enableStartAudioStream() {
    this.audioFeed.enable()
    FurhatAudioFeedStreamer.start(params.ROBOT_IP_ADDRESS)
    recorder.startRecording()
}

fun StateBuilder.onUserSilence(trigger: TriggerRunner<*>.(NoSpeechDetected) -> Unit) {
    onEvent<NoSpeechDetected> {
        trigger.invoke(this, it)
    }
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
    val lang = this.inputLanguages.first()
    EventSystem.send(ListenStarted())
    audioStreamToEvent(lang, recorder, timeout, endSil, maxSpeech)
    runner.call(listenState(timeout, endSil, maxSpeech))
}

fun listenState(timeout: Long, endSil: Long, maxSpeech: Long) = state {
    var speechWasRecognized = false
    var lastSpeechTime = -1L
    val logger = CommonUtils.getLogger("ListenState")

    /**
     * Checks how long ago the last speech was recognized, ends the stream if its longer than the endSil param.
     */
    onTime(repeat = 100, instant = true, cond = { speechWasRecognized }) {
        if(
            lastSpeechTime != -1L &&
            System.currentTimeMillis() - lastSpeechTime > endSil
        ) {
            logger.info("endSil for listen reached.")
            recorder.audioStreamingStopped()
        }
    }

    /**
     * If it takes longer than initialTimeout for speech to be recognized, end the stream.
     */
    onTime(delay = timeout.toInt(), instant = true, cond = { !speechWasRecognized} ) {
        logger.info("timeout for listen reached.")
        recorder.startRecording()
    }

    /**
     * End the stream if maxSpeech timeout is reached.
     */
    onTime(delay = maxSpeech.toInt(), instant = true) {
        logger.info("maxSpeech for listen reached.")
        recorder.startRecording()
    }

    onExit {
        logger.info("Exiting listen state")
        recorder.startRecording()
    }
}