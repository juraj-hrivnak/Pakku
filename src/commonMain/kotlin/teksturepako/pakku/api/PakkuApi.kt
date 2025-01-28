package teksturepako.pakku.api

import teksturepako.pakku.api.http.client
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object PakkuApi
{
    data class Configuration(
        internal var developmentMode: Boolean = false,
        internal var curseForgeApiKey: String? = null,
        internal var userAgent: String? = null,
        internal var timeout: Duration = 3.minutes,
    )
    {
        fun developmentMode()
        {
            this.developmentMode = true
        }

        fun curseForge(apiKey: String)
        {
            this.curseForgeApiKey = apiKey
        }

        fun withUserAgent(agent: String)
        {
            this.userAgent = agent
        }

        fun withTimeout(timeout: Duration)
        {
            this.timeout = timeout
        }

        internal fun init()
        {
            configuration?.let { config ->
                if (developmentMode)
                {
                    println("Pakku is running in development mode")
                }
                else
                {
                    requireNotNull(config.curseForgeApiKey) { "curseForgeApiKey must be specified" }
                    requireNotNull(config.userAgent) { "userAgent must be specified" }
                }
            } ?: throw IllegalStateException("PakkuApi must be configured before use")
        }
    }

    var configuration: Configuration? = null
        private set
        get() = field?.copy()

    internal fun configure(block: Configuration.() -> Unit)
    {
        if (configuration == null)
        {
            configuration = Configuration().apply(block)
            configuration!!.init()
        }
    }

    fun isConfigured(): Boolean = configuration != null
}

fun initPakku(block: PakkuApi.Configuration.() -> Unit)
{
    PakkuApi.configure(block)
}

fun executePakku(block: () -> Unit): Boolean
{
    if (PakkuApi.isConfigured())
    {
        block()
        client.close()
    }

    return PakkuApi.isConfigured()
}
