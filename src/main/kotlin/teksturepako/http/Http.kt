package teksturepako.http

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import teksturepako.debug

open class Http
{
    /**
     * Returns body InputStream of a https request.
     */
    open suspend fun requestBody(url: String): String?
    {
        return client.get(url).butOnlyIfStatusCodeOK()
    }

    /**
     * Returns body InputStream of a https request, with headers provided.
     */
    suspend fun requestBody(url: String, vararg headerEntries: Pair<String, String>): String? {
        return client.get(url) {
            headerEntries.forEach { headers.append(it.first, it.second) }
        }.butOnlyIfStatusCodeOK()
    }

    private suspend fun HttpResponse.butOnlyIfStatusCodeOK(): String?
    {
        return if (this.status == HttpStatusCode.OK) this.body() else null.debug {
            println("Bad request for $this }")
        }
    }
}

