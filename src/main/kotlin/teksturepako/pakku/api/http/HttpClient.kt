package teksturepako.pakku.api.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import teksturepako.pakku.api.data.json

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json
    }
    install(HttpTimeout) {
        socketTimeoutMillis = 600000
    }
    install(UserAgent) {
        agent = "juraj-hrivnak/Pakku (tekeksturepako@gmail.com)"
    }
    engine {
        threadsCount = 30
        pipelining = true
        config {
            retryOnConnectionFailure(true)
        }
    }
}