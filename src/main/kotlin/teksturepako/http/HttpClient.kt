package teksturepako.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*

val client = HttpClient(OkHttp) {
    install(HttpTimeout) {
        socketTimeoutMillis = 600000
    }
    engine {
        threadsCount = 30
        pipelining = true
        config {
            retryOnConnectionFailure(true)
        }
    }
}