package teksturepako.pakku.io

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal const val FILE_IO_CONCURRENCY = 8

suspend inline fun <T, R> Iterable<T>.mapAsync(
    crossinline transform: suspend (T) -> R
): List<R> = mapAsync(Int.MAX_VALUE, transform)

suspend inline fun <T, R> Iterable<T>.mapAsync(
    concurrency: Int,
    crossinline transform: suspend (T) -> R
): List<R> = coroutineScope {
    require(concurrency > 0) { "concurrency must be positive" }

    val semaphore = Semaphore(concurrency)
    this@mapAsync.map { item ->
        async {
            semaphore.withPermit {
                transform(item)
            }
        }
    }.awaitAll()
}

suspend inline fun <T, R : Any> Iterable<T>.mapAsyncNotNull(
    crossinline transform: suspend (T) -> R?
): List<R> = this.mapAsync(transform).filterNotNull()

suspend inline fun <T, R : Any> Iterable<T>.mapAsyncNotNull(
    concurrency: Int,
    crossinline transform: suspend (T) -> R?
): List<R> = this.mapAsync(concurrency, transform).filterNotNull()
