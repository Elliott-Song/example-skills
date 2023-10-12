package furhatos.app.customasr

import furhatos.app.customasr.flow.Basic
import furhatos.flow.kotlin.Flow
import furhatos.skills.Skill
// import sagemaker
import aws.sdk.kotlin.services.sagemakerruntime.SageMakerRuntimeClient
import aws.sdk.kotlin.services.sagemakerruntime.model.InvokeEndpointRequest
import com.google.gson.Gson

class CustomasrSkill : Skill() {
    override fun start() {
        Flow().run(Basic)
    }
}

suspend fun main(args: Array<String>) {
    Skill.main(args)
//    val wavFilePath = "C:\\Users\\Mastr\\Tech\\NorwAI\\tutorials\\Norwegian.wav"
//    val wavFileByteArray : ByteArray = java.io.File(wavFilePath).readBytes()
//    val invokeEndpointRequest = InvokeEndpointRequest {
//        endpointName = "jumpstart-dft-hf-asr-whisper-medium"
//        contentType = "audio/wav"
//        body = wavFileByteArray
//    }
//    // Oslo har ut fra sin geografiske beliggenhet innerst i Oslofjorden vært et naturlig knutepunkt mellom sjøen, de store landområdene nord for byen og landeveien sørfra over Ekeberg.
//    // Oslo har ut fra sin geografiske beliggenhet innerst i Oslofjorden vært et naturlig knutepunkt mellom sjøen, de store landområdene nord for byen og landeveien sør fra over Ekeberg.
//    SageMakerRuntimeClient { region = "eu-north-1"}.use { sageMakerRuntimeClient ->
//        val response = sageMakerRuntimeClient.invokeEndpoint(invokeEndpointRequest)
//        // decode Map from byteArray
//        val map = Gson().fromJson(response.body?.toString(charset = Charsets.UTF_8), Map::class.java)
//        val text = map["text"]
//        println(text)
//    }
}
