package teksturepako.data

import kotlinx.serialization.json.Json

val json = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}

fun Any?.finalize(): String
{
    return this.toString().replace("\"", "")
}