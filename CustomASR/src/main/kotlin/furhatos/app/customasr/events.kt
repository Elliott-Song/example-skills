package furhatos.app.customasr

import furhatos.event.Event

/**
 * Events send by [TranscriptBehavior]
 */
open class ListenDone(val finalText: String): Event()
open class ListenStarted: Event()

/**
 * Base Intent event
 */
open class customListenDone(
        val text: String
) : Event()
open class NoSpeechDetected: Event() // No Speech

open class InterimResult(): Event()