package teksturepako.pakku.api.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import teksturepako.pakku.debug

open class Http
{
    /**
     * @return A body [ByteArray] of a https request or null if status code is not OK.
     */
    open suspend fun requestByteArray(url: String): ByteArray?
    {
        return client.get(url).bodyIfOK()
    }

    /**
     * @return A body [String] of a https request or null if status code is not OK.
     */
    open suspend fun requestBody(url: String): String?
    {
        return client.get(url).bodyIfOK()
    }

    /**
     * @return A body [String] of a https request, with headers provided or null if status code is not OK.
     */
    suspend fun requestBody(url: String, vararg headerEntries: Pair<String, String>): String?
    {
        return client.get(url) {
            headerEntries.forEach { headers.append(it.first, it.second) }
        }.bodyIfOK()
    }

    suspend inline fun <reified T> HttpResponse.bodyIfOK(): T?
    {
        return if (this.status == HttpStatusCode.OK) this.body() else null.debug {
            println("Bad request for $this")
        }
    }
}

