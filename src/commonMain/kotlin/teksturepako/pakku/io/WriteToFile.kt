package teksturepako.pakku.io

import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.json
import kotlin.io.path.*

inline fun <reified T> writeToFile(
    value: T,
    path: String,
    overrideText: Boolean = false,
    format: StringFormat = json
) = runCatching {
    val file = Path(path)

    // Override file text
    if (overrideText && file.exists()) file.deleteIfExists()

    // Write to file
    file.parent.createParentDirectories()
    file.writeText(format.encodeToString(value))
}