package furhatos.app.customasr.flow

import furhatos.app.customasr.nlu.*
import furhatos.flow.kotlin.*
import furhatos.nlu.Intent
import furhatos.util.Language

val customListening = partialState {
    onEvent<customListenDone> { // needed to be able to use the customASR
        furhat.say(it.text)
        val results = thisState.getIntentClassifier(lang = Language.ENGLISH_US).classify(it.text)
        results.forEach {
            println("Intent: ${it.intents.toString()} - Confidence: ${it.conf}")
        }
        if (results.isEmpty()) {
            raise(Intent()) // trigger default response
        } else {
            raise(results.first().intents.first()) // trigger the first intent
        }
    }
}

