package furhatos.app.customasr.nlu

import furhatos.app.customasr.ListenDone
import furhatos.app.customasr.ListenStarted
import furhatos.event.EventSystem
import furhatos.flow.kotlin.state
import furhatos.util.CommonUtils

private val logger = CommonUtils.getLogger("ASR-EventListener")
/**
 * Listens to event send by the [furhatos.app.customasr.com.getTranscriptor]
 */
val ListenState = state {
    var fullText = ""
    var listenEnded = false
    var rms = 0.0

    onEvent<ListenStarted>(instant = true) {
        logger.info("A new listen has started, resetting state.")
        fullText = ""
        listenEnded = false
        rms = 0.0
    }

    onEvent<ListenDone>(instant = true, cond = {!listenEnded}) {
        fullText = it.finalText
        listenEnded = true
        var eventSend = false
        logger.info("Listen done")
        logger.info(fullText)

        if (fullText.isEmpty()) {
            EventSystem.send(NoSpeechDetected())
        } else {
            /**
             * It would be wise to implement a smarter NLU here.
             */
            NLUList.forEach { (example, constructor) ->
                if(fullText.contains(example, ignoreCase = true)) {
                    eventSend = true
                    EventSystem.send(
                        constructor.invoke(fullText, rms) // Send the specific Intent Event
                    )
                }
            }
            if (!eventSend) {
                EventSystem.send(TextAndMetrics(fullText, rms)) // If no specific intent was sent, send a generic one.
            }
        }
    }
}