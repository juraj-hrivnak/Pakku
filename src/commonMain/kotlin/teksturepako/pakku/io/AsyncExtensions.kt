package teksturepako.pakku.io

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend inline fun <T, R> Iterable<T>.mapAsync(
    crossinline transform: suspend (T) -> R
): List<R> = coroutineScope {
    val mutex = Mutex()
    this@mapAsync.map { item ->
        mutex.withLock {
            async {
                transform(item)
            }
        }
    }.awaitAll()
}

suspend inline fun <T, R : Any> Iterable<T>.mapAsyncNotNull(
    crossinline transform: suspend (T) -> R?
): List<R> = this.mapAsync(transform).filterNotNull()
