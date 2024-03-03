package teksturepako.pakku.io

import korlibs.io.file.std.localCurrentDirVfs
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.data.json

suspend fun readFileOrNull(path: String): String?
{
    val file = localCurrentDirVfs[path]

    return if (file.exists()) {
        runCatching { file.readString() }.getOrNull()
    } else null
}

suspend inline fun <reified T> decodeOrNew(value: T, path: String): T = readFileOrNull(path)?.let {
    runCatching { json.decodeFromString<T>(it) }.getOrElse { value }
} ?: value

suspend inline fun <reified T> decodeToResult(value: T, path: String): Result<T> = readFileOrNull(path)?.let {
    runCatching { Result.success(json.decodeFromString<T>(it)) }.getOrElse { exception ->
        Result.failure(PakkuException("Error occurred while reading '$path': ${exception.message}"))
    }
} ?: Result.failure(PakkuException("Could not read '$path'"))