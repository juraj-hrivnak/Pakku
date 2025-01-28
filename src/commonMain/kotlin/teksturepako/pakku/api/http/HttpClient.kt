package teksturepako.pakku.api.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import teksturepako.pakku.api.PakkuApi
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        Json
    }
    install(HttpTimeout) {
        val timeout = PakkuApi.configuration?.timeout?.inWholeMilliseconds

        socketTimeoutMillis = timeout
        requestTimeoutMillis = timeout
        connectTimeoutMillis = timeout
    }
    install(UserAgent) {
        PakkuApi.configuration?.userAgent?.let { agent = it }
    }
    engine {
        pipelining = true
        config {
            val timeout = PakkuApi.configuration?.timeout?.toJavaDuration() ?: 1.minutes.toJavaDuration()

            retryOnConnectionFailure(true)

            connectTimeout(timeout)
            callTimeout(timeout)
            writeTimeout(timeout)
        }
    }
}
