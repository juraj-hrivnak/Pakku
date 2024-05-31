package teksturepako.pakku.api.http

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import teksturepako.pakku.debug

open class Http
{
    /**
     * @return A body [ByteArray] of a https request or null if status code is not OK.
     */
    open suspend fun requestByteArray(
        url: String, onDownload: (bytesSentTotal: Long, contentLength: Long) -> Unit = { _: Long, _: Long ->}
    ): ByteArray?
    {
        return try
        {
            client.get(url) {
                onDownload { bytesSentTotal, contentLength -> onDownload(bytesSentTotal, contentLength) }
            }
                .debug { println("${this::class.simpleName} $it") }
                .checkLimit()
                .bodyIfOK()
        }
        catch (e: Exception)
        {
            println("Error: ${this::class.simpleName} ${e.printStackTrace()}")
            null
        }
    }

    /**
     * @return A body [String] of a https request or null if status code is not OK.
     */
    open suspend fun requestBody(url: String): String?
    {
        return try
        {
            client.get(url)
                .debug { println("${this::class.simpleName} $it") }
                .checkLimit()
                .bodyIfOK()
        }
        catch (e: Exception)
        {
            println("Error: ${this::class.simpleName} ${e.printStackTrace()}")
            null
        }
    }

    /**
     * @return A body [String] of a https request, with headers provided or null if status code is not OK.
     */
    suspend fun requestBody(url: String, vararg headers: Pair<String, String>): String?
    {
        return try
        {
            client.get(url) { headers.forEach { this.headers.append(it.first, it.second) } }
                .debug { println("${this::class.simpleName} $it") }
                .checkLimit()
                .bodyIfOK()
        }
        catch (e: Exception)
        {
            println("Error: ${this::class.simpleName} ${e.printStackTrace()}")
            null
        }
    }

    suspend inline fun <reified T> requestBody(url: String, bodyContent: T): String?
    {
        return try
        {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(bodyContent)) // Do not use pretty print
            }
                .debug { println("${this::class.simpleName} ${it.call} ${it.request.content}") }
                .checkLimit()
                .bodyIfOK()
        }
        catch (e: Exception)
        {
            println("Error: ${this::class.simpleName} ${e.printStackTrace()}")
            null
        }
    }

    open suspend fun HttpResponse.checkLimit(): HttpResponse = this

    suspend inline fun <reified T> HttpResponse.bodyIfOK(): T?
    {
        return if (this.status == HttpStatusCode.OK) this.body() else null.debug {
            println("Error: ${this@Http::class.simpleName} HTTP request returned: ${this.status}")
        }
    }
}
