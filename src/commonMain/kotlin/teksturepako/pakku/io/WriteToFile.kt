package teksturepako.pakku.io

import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.json

suspend inline fun <reified T> writeToFile(
    value: T,
    path: String,
    overrideText: Boolean = false,
    format: StringFormat = json
)
{
    val file = localCurrentDirVfs[path]

    // Override file text
    if (overrideText) file.delete()

    // Write to file
    file.writeString(format.encodeToString(value))
}