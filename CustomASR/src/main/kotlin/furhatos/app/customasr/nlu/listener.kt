package furhatos.app.customasr.nlu

import furhatos.app.customasr.ListenDone
import furhatos.app.customasr.ListenStarted
import furhatos.event.EventSystem
import furhatos.flow.kotlin.*
import furhatos.util.CommonUtils

private val logger = CommonUtils.getLogger("ASR-EventListener")
/**
 * Listens to event send by the [furhatos.app.customasr.audiofeed.TranscriptBehaviorKt.audioStreamToEvent]
 */

val ListenState = state {
    var fullText = ""
    var listenEnded = false

    onEvent<ListenStarted>(instant = true) {
        logger.info("A new listen has started, resetting state.")
        fullText = ""
        listenEnded = false
    }

    onEvent<ListenDone>(instant = true, cond = {!listenEnded}) { it ->
        fullText = it.finalText
        listenEnded = true
        logger.info("Listen done")
        logger.info(fullText)

        if (fullText.isEmpty()) {
            EventSystem.send(NoSpeechDetected())
        } else {
            EventSystem.send(customListenDone(fullText))
        }
    }
}