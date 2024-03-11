package teksturepako.pakku.io

import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.data.json

suspend fun readFileOrNull(path: String): String?
{
    val file = localCurrentDirVfs[path]

    return if (file.exists()) {
        runCatching { file.readString() }.getOrNull()
    } else null
}

suspend inline fun <reified T> decodeOrNew(
    value: T, path: String, format: StringFormat = json
): T = readFileOrNull(path)?.let {
    runCatching { format.decodeFromString<T>(format.serializersModule.serializer(), it) }.getOrElse { value }
} ?: value

suspend inline fun <reified T> decodeToResult(
    path: String, format: StringFormat = json
): Result<T> = readFileOrNull(path)?.let {
    runCatching { Result.success(format.decodeFromString<T>(format.serializersModule.serializer(), it)) }.getOrElse { exception ->
        Result.failure(PakkuException("Error occurred while reading '$path': ${exception.message}"))
    }
} ?: Result.failure(PakkuException("Could not read '$path'"))