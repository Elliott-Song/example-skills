package furhatos.app.attentiongrabber.flow

import furhatos.app.attentiongrabber.flow.main.Idle
import furhatos.app.attentiongrabber.setting.activate
import furhatos.app.attentiongrabber.setting.mainPersona
import furhatos.app.attentiongrabber.setting.maxNumberOfUsers
import furhatos.event.actions.ActionGaze
import furhatos.flow.kotlin.furhat
import furhatos.flow.kotlin.state
import furhatos.flow.kotlin.users
import furhatos.gestures.Gestures


val Init = state {
    onEntry {
//        /** Set our default interaction parameters */
//        users.setSimpleEngagementPolicy(0.5, 1.2, 1.2, 1.7, maxNumberOfUsers)
//
//        /** Set our main character - defined in personas */
//        activate(mainPersona)
//
//        /** start the interaction */
//        goto(Idle)
        furhat.ledStrip.solid(java.awt.Color(0,0,127))
        // wait for 5 seconds
        furhat.gesture(Gestures.CloseEyes)
        delay(5000)
        furhat.ledStrip.solid(java.awt.Color(127,127,127))
        // look surprised
        furhat.gesture(Gestures.OpenEyes)
        furhat.gesture(Gestures.Oh)
        furhat.say("Oh hello there.")
        furhat.gesture(Gestures.ExpressFear)
        furhat.say("I heard it's your birthday today.")
        furhat.gesture(Gestures.BigSmile)
        furhat.say("Happy birthday to you father.")
        furhat.gesture(Gestures.BigSmile)
        furhat.setVisibility(false, 1000)
    }
}