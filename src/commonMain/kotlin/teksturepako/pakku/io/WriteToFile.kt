package teksturepako.pakku.io

import com.github.michaelbull.result.getError
import com.github.michaelbull.result.onFailure
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.json
import kotlin.io.path.*

suspend inline fun <reified T> writeToFile(
    value: T,
    path: String,
    overrideText: Boolean = false,
    format: StringFormat = json
): ActionError?
{
    val file = Path(path)
    val backup = file.tryOrNull { it.readBytes() }

    return file.tryToResult {
        if (overrideText) it.deleteIfExists()

        runCatching { it.parent.createParentDirectories() }
        it.writeText(format.encodeToString(value))
    }.onFailure {
        // Restore backup if there was an error
        backup?.let { bytes ->
            file.deleteIfExists()
            file.tryOrNull { it.writeBytes(bytes) }
        }
    }.getError()
}