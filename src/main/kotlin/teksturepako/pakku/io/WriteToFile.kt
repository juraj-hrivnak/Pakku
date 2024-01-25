package teksturepako.pakku.io

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToStream
import okio.FileSystem
import okio.Path
import teksturepako.pakku.api.data.json
import java.io.File
import java.io.FileOutputStream

inline fun <reified T : Any> writeToPath(value: T, path: Path, overrideText: Boolean = false)
{
    if (overrideText) FileSystem.SYSTEM.sink(path).flush()

    FileSystem.SYSTEM.write(path) {
        writeUtf8(json.encodeToString(value))
    }
}

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