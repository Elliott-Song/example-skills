package furhatos.app.customasr.nlu

import furhatos.event.Event
import furhatos.nlu.Intent
import furhatos.util.Language

/**
 * Base Intent event
 */
open class customListenDone(
    val text: String
) : Event()
open class NoSpeechDetected: Event() // No Speech

class HowDoIWork : Intent() {
    override fun getConfidenceThreshold(): Double {
        return 0.8
    }

    override fun getExamples(lang: Language): List<String> {
        return listOf(
                "can you tell me how you work",
                "how do you work",
                "can you explain how you work",
                "what do you do when answering questions",
                "can you explain your architecture")
    }

    override fun getNegativeExamples(lang: Language): List<String> {
        return listOf(
                "can you tell me a joke",
                "can you tell me something about AI",
                "What is your name",
                "Do you have a name"
        )
    }
}

class WhatCanIAsk : Intent() {
    override fun getConfidenceThreshold(): Double {
        return 0.8
    }

    override fun getExamples(lang: Language): List<String> {
        if (lang == Language.NORWEGIAN) {
            return listOf("hva kan du fortelle meg", "hva kan du si meg", "hva har du informasjon om",  "hva vet du?", "hvilke sider har du kunnskap om?", "hvilke sider vet du om?", "hva kan jeg spørre deg om?")
        }

        return listOf(
                "what can you tell me about",
                "what are your options",
                "what can I ask you",
                "what can I ask you about",
                "what can you tell me",
                "what do you know"
        )
    }

    override fun getNegativeExamples(lang: Language): List<String> {
        return listOf(
                "what is the population of Trondheim"
        )
    }
}
