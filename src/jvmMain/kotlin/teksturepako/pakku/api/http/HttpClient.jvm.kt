package teksturepako.pakku.api.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Suppress("DEPRECATION")
actual val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        Json
    }
    install(HttpTimeout) {
        socketTimeoutMillis = 3.minutes.inWholeMilliseconds
        requestTimeoutMillis = 3.minutes.inWholeMilliseconds
        connectTimeoutMillis = 3.minutes.inWholeMilliseconds
    }
    install(UserAgent) {
        agent = "juraj-hrivnak/Pakku (tekeksturepako@gmail.com)"
    }
    engine {
        threadsCount = 30
        pipelining = true
        config {
            retryOnConnectionFailure(true)

            connectTimeout(3.minutes.toJavaDuration())
            callTimeout(3.minutes.toJavaDuration())
            writeTimeout(3.minutes.toJavaDuration())
        }
    }
}