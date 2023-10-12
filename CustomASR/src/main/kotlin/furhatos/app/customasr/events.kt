package furhatos.app.customasr

import furhatos.event.Event

/**
 * Events send by [TranscriptBehavior]
 */
open class ListenDone(val finalText: String): Event()
open class ListenStarted: Event()
