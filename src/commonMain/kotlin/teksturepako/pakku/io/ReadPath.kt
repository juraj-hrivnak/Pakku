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
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText


suspend fun <T> Path.tryOrNull(action: (path: Path) -> T): T? = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext runCatching { action(this@tryOrNull) }.get()
    }
}

suspend fun <T> Path.tryToResult(action: (path: Path) -> T): Result<T, ActionError> = coroutineScope {
    withContext(Dispatchers.IO) {
        return@withContext runCatching { action(this@tryToResult) }.fold(
            success = { Ok(it) },
            failure = { Err(ErrorWhileReading(this@tryToResult.toString(), it.stackTraceToString())) }
        )
    }
}

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

suspend fun readPathToResult(path: Path): Result<String, ActionError> = coroutineScope {
    return@coroutineScope withContext(Dispatchers.IO) {
        if (path.exists()) runCatching { path.readText() }.fold(
            success = { Ok(it) },
            failure = { Err(CouldNotRead(path.toString(), it.stackTraceToString())) }
        )
        else Err(FileNotFound(path.toString()))
    }
}

suspend fun readPathBytesToResult(path: Path): Result<ByteArray, ActionError> = coroutineScope {
    return@coroutineScope withContext(Dispatchers.IO) {
        if (path.exists()) runCatching { path.readBytes() }.fold(
            success = { Ok(it) },
            failure = { Err(CouldNotRead(path.toString(), it.stackTraceToString())) }
        )
        else Err(FileNotFound(path.toString()))
    }
}

suspend inline fun <reified T> decodeToResult(path: Path, format: StringFormat = json): Result<T, ActionError>
{
    val file = readPathToResult(path).getOrElse { return Err(it) }

    return runCatching<T> { format.decodeFromString(format.serializersModule.serializer(), file) }.fold(
        success = { Ok(it) },
        failure = { Err(ErrorWhileReading(file, it.stackTraceToString())) }
    )
}
