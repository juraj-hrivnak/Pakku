package teksturepako.pakku.api.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.debug
import teksturepako.pakku.limit
import kotlin.system.exitProcess

open class Http
{
    /**
     * @return A body [ByteArray] of a https request or null if status code is not OK.
     */
    open suspend fun requestByteArray(url: String): ByteArray?
    {
        return client.get(url)
            .debug { println("${this.javaClass.simpleName} $it") }
            .checkLimit()
            .bodyIfOK()
    }

    /**
     * @return A body [String] of a https request or null if status code is not OK.
     */
    open suspend fun requestBody(url: String): String?
    {
        return client.get(url)
            .debug { println("${this.javaClass.simpleName} $it") }
            .checkLimit()
            .bodyIfOK()
    }

    /**
     * @return A body [String] of a https request, with headers provided or null if status code is not OK.
     */
    suspend fun requestBody(url: String, vararg headers: Pair<String, String>): String?
    {
        return client.get(url) { headers.forEach { this.headers.append(it.first, it.second) } }
            .debug { println("${this.javaClass.simpleName} $it") }
            .checkLimit()
            .bodyIfOK()
    }

    suspend inline fun <reified T> requestBody(url: String, bodyContent: T): String?
    {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(bodyContent)) // Don't use pretty print
        }
            .debug { println("${this.javaClass.simpleName} ${it.call} ${it.request.content}") }
            .checkLimit()
            .bodyIfOK()
    }

    suspend fun HttpResponse.checkLimit(): HttpResponse
    {
        this.headers["x-ratelimit-remaining"]?.toInt()?.let {
            limit = it
            if (it == 0)
            {
                print("Error: ")
                println(json.decodeFromString<JsonObject>(this.body())["description"].finalize())
                exitProcess(1)
            }
        }
        return this
    }

    suspend inline fun <reified T> HttpResponse.bodyIfOK(): T?
    {
        return if (this.status == HttpStatusCode.OK) this.body() else null
    }
}

