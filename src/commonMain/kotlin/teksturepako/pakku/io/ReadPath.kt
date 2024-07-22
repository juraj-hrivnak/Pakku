package teksturepako.pakku.io

import com.github.michaelbull.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.json
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText

fun Throwable.toActionError(path: Path): ActionError = when (this)
{
    is FileAlreadyExistsException -> AlreadyExists(path.toString())
    is NoSuchFileException        -> FileNotFound(path.toString())
    else -> ErrorWhileReading(path.toString(), this.stackTraceToString())
}

suspend fun <T> Path.tryOrNull(action: (path: Path) -> T): T? = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext runCatching { action(this@tryOrNull) }.get()
    }
}

suspend fun <T> Path.tryToResult(action: (path: Path) -> T): Result<T, ActionError> = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext runCatching { action(this@tryToResult) }.fold(
            success = { Ok(it) },
            failure = { Err(it.toActionError(this@tryToResult)) }
        )
    }
}

@Suppress("unused")
suspend fun readPathOrNull(path: Path): String? = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext if (path.exists()) runCatching { path.readText() }.get() else null
    }
}

suspend fun readPathBytesOrNull(path: Path): ByteArray? = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext if (path.exists()) runCatching { path.readBytes() }.get() else null
    }
}

suspend fun readPathToResult(path: Path): Result<Pair<Path, String>, ActionError> = coroutineScope {
    return@coroutineScope withContext(Dispatchers.IO) {
        if (path.exists()) runCatching { path to path.readText() }.fold(
            success = { Ok(it) },
            failure = { Err(CouldNotRead(path.toString(), it.stackTraceToString())) }
        )
        else Err(FileNotFound(path.toString()))
    }
}

@Suppress("unused")
suspend fun readPathBytesToResult(path: Path): Result<ByteArray, ActionError> = coroutineScope {
    return@coroutineScope withContext(Dispatchers.IO) {
        if (path.exists()) runCatching { path.readBytes() }.fold(
            success = { Ok(it) },
            failure = { Err(CouldNotRead(path.toString(), it.stackTraceToString())) }
        )
        else Err(FileNotFound(path.toString()))
    }
}

@Suppress("unused")
suspend inline fun <reified T> decodeToResult(inputPath: Path, format: StringFormat = json): Result<T, ActionError>
{
    val (file, text) = readPathToResult(inputPath).getOrElse { return Err(it) }

    return runCatching<T> { format.decodeFromString(format.serializersModule.serializer(), text) }.fold(
        success = { Ok(it) },
        failure = { Err(it.toActionError(file)) }
    )
}
