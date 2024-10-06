package teksturepako.pakku.io

import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.jsonSnakeCase
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText

fun readPathTextOrNull(path: String): String?
{
    return runCatching { Path(path).readText() }.getOrNull()
}

fun readPathBytesOrNull(path: String): ByteArray?
{
    return runCatching { Path(path).readBytes() }.getOrNull()
}

inline fun <reified T> decodeOrNew(
    value: T, path: String, format: StringFormat = jsonSnakeCase
): T = readPathTextOrNull(path)?.let {
    runCatching { format.decodeFromString<T>(format.serializersModule.serializer(), it) }.getOrElse { value }
} ?: value

inline fun <reified T> decodeToResult(
    path: String, format: StringFormat = jsonSnakeCase
): Result<T> = readPathTextOrNull(path)?.let {
    runCatching { Result.success(format.decodeFromString<T>(format.serializersModule.serializer(), it)) }.getOrElse { exception ->
        Result.failure(PakkuException("Error occurred while reading '$path': ${exception.message}"))
    }
} ?: Result.failure(PakkuException("Could not read '$path'"))
