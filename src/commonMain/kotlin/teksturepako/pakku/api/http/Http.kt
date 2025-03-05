@file:Suppress("MemberVisibilityCanBePrivate")

package teksturepako.pakku.api.http

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.debug

class RequestError(val response: HttpResponse) : ActionError()
{
    override val rawMessage = message(
        "HTTP request error ",
        "returned ${response.status.value} ",
        response.status.description
    )
}

class ConnectionError(val exception: Exception) : ActionError()
{
    override val rawMessage = "HTTP connection error ${exception.message}"
}

suspend inline fun <reified T> tryRequest(block: () -> HttpResponse): Result<T, ActionError>
{
    return try
    {
        val response = block()

        when (response.status)
        {
            HttpStatusCode.OK -> Ok(response.body<T>())
            else              -> Err(RequestError(response))
        }
    }
    catch (e: Exception)
    {
        debug { e.printStackTrace() }
        Err(ConnectionError(e))
    }
}

/**
 * @return A body [ByteArray] of a https request or null if status code is not OK.
 */
suspend fun requestByteArray(
    url: String,
    onDownload: suspend (bytesSentTotal: Long, contentLength: Long?) -> Unit = { _: Long, _: Long? -> }
): Result<ByteArray, ActionError> = tryRequest {
    pakkuClient.get(url) {
        onDownload { bytesSentTotal, contentLength -> onDownload(bytesSentTotal, contentLength) }
    }
}

/**
 * @return A body [String] of a https request or null if status code is not OK.
 */
suspend inline fun requestBody(url: String): Result<String, ActionError> = tryRequest { pakkuClient.get(url) }

/**
 * @return A body [String] of a https request, with headers provided or null if status code is not OK.
 */
suspend inline fun requestBody(
    url: String,
    vararg headers: Pair<String, String>
): Result<String, ActionError> = tryRequest {
    pakkuClient.get(url) { headers.forEach { this.headers.append(it.first, it.second) } }
}

suspend inline fun requestBody(
    url: String,
    bodyContent: () -> String
): Result<String, ActionError> = tryRequest {
    pakkuClient.post(url) {
        contentType(ContentType.Application.Json)
        setBody(bodyContent()) // Do not use pretty print
    }
}

/**
 * @return A body [String] of a https request, with headers provided or null if status code is not OK.
 */
suspend inline fun requestBody(
    url: String, bodyContent: () -> String,
    vararg headers: Pair<String, String>
): Result<String, ActionError> = tryRequest {
    pakkuClient.post(url) {
        headers.forEach { this.headers.append(it.first, it.second) }

        contentType(ContentType.Application.Json)
        setBody(bodyContent()) // Do not use pretty print
    }
}
