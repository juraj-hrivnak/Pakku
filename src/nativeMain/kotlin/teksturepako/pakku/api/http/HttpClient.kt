package teksturepako.pakku.api.http

import io.ktor.client.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

val client = HttpClient(Curl) {
    install(ContentNegotiation) {
        Json
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
        sslVerify = false
    }
}