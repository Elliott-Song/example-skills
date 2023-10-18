package furhatos.app.customasr

import furhatos.event.Event


open class ListenStarted(
        val timeout: Long,
        val endSil: Long,
        val maxSpeech: Long
): Event()

open class customListenDone(
        val text: String
) : Event()
open class NoSpeechDetected: Event() // No Speech

open class InterimResult(): Event()