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
    EventSystem.send(ListenStarted())
    audioStreamToEvent(recorder, timeout, endSil, maxSpeech)
}