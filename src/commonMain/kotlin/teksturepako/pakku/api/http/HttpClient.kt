package teksturepako.pakku.api.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        Json
    }
    install(HttpTimeout) {
        val timeout = 3.minutes.inWholeMilliseconds

        socketTimeoutMillis = timeout
        requestTimeoutMillis = timeout
        connectTimeoutMillis = timeout
    }
    install(UserAgent) {
        agent = "juraj-hrivnak/Pakku (tekeksturepako@gmail.com)"
    }
    engine {
        pipelining = true
        config {
            retryOnConnectionFailure(true)

            connectTimeout(3.minutes.toJavaDuration())
            callTimeout(3.minutes.toJavaDuration())
            writeTimeout(3.minutes.toJavaDuration())
        }
    }
}
