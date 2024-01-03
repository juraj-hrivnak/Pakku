package teksturepako.pakku.io

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import teksturepako.pakku.api.data.json
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> writeToFile(value: T, file: File, overrideText: Boolean = false)
{
    // Override file text
    if (overrideText) FileOutputStream(file).close()

    // Write to file
    FileOutputStream(file).use {
        json.encodeToStream(value, it)
    }
}