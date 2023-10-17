package furhatos.app.customasr.flow

import furhatos.app.customasr.customListenDone
import furhatos.app.customasr.extensions.*
import furhatos.app.customasr.nlu.*
import furhatos.flow.kotlin.*

/**
 * The state shows how a CustomASR could be used with custom extension functions.
 *  Listens using [customListenDone]
 *  When no speech is recognized, the [onUserSilence] is triggered.
 */
val Basic: State = state {
    include(customListening)

    init {
        furhat.enableStartAudioStream() // Start the stream and listener
        parallel(ParallelListenState, false) // Start the state in charge of Listening and NLU.
    }

    onButton("Ask") {
        furhat.say("Hello there!")
        furhat.customListen()
    }

    onResponse<HowDoIWork> {
        furhat.say("I work by listening to your voice and trying to understand what you say.")
    }
    onResponse<WhatCanIAsk> {
        furhat.say("You can ask me anything you want.")
    }
    onResponse {
        furhat.say("I don't know what you said.")
        // for testing customAskYN
        val cont = furhat.customAskYN("Can you say yes or no?")
        if (cont) {
            furhat.say("Ok, you said yes!")
        } else {
            furhat.say("Ok, you said no!")
        }
    }
    onUserSilence {
        furhat.say("You said nothing!")
    }
}

