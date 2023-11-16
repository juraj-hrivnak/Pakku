package teksturepako.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import teksturepako.debug

open class Http
{
    /**
     * Returns body [String] of a https request or null if status code is not OK.
     */
    open suspend fun requestBody(url: String): String?
    {
        return client.get(url).bodyIfOK()
    }
    
    /**
     * Returns body [String] of a https request, with headers provided or null if status code is not OK.
     */
    suspend fun requestBody(url: String, vararg headerEntries: Pair<String, String>): String? {
        return client.get(url) {
            headerEntries.forEach { headers.append(it.first, it.second) }
        }.bodyIfOK()
    }

    private suspend fun HttpResponse.bodyIfOK(): String?
    {
        return if (this.status == HttpStatusCode.OK) this.body() else null.debug {
            println("Bad request for $this")
        }
    }
}

