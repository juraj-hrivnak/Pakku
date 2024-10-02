package teksturepako.pakku.api.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
    classDiscriminator = "_internal"
    explicitNulls = false
}

val jsonEncodeDefaults = Json {
    prettyPrint = true
    classDiscriminator = "_internal"
    encodeDefaults = true
}

