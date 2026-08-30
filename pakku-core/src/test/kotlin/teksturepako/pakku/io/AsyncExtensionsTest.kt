package teksturepako.pakku.io

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.isLessThanOrEqualTo
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class AsyncExtensionsTest
{
    @Test
    fun `mapAsync respects its concurrency limit`(): Unit = runBlocking {
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()

        (1..8).mapAsync(concurrency = 2) {
            val current = active.incrementAndGet()
            maximumActive.accumulateAndGet(current, ::maxOf)
            delay(10)
            active.decrementAndGet()
        }

        expectThat(maximumActive.get()).isLessThanOrEqualTo(2)
    }
}
