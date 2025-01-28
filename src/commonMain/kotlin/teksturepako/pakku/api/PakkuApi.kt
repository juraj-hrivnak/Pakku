package teksturepako.pakku.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object PakkuApi
{
    data class Configuration(
        var developmentMode: Boolean = false,
        var curseForgeApiKey: String? = null,
        var userAgent: String? = null,
        var timeout: Duration = 3.minutes,
    )
    {
        /** Enables development mode for testing purposes. */
        fun developmentMode()
        {
            this.developmentMode = true
        }

        /** Sets the CurseForge API key for authentication. */
        fun curseForge(apiKey: String)
        {
            this.curseForgeApiKey = apiKey
        }

        /** Sets the user agent for HTTP requests. */
        fun withUserAgent(agent: String)
        {
            this.userAgent = agent
        }

        /** Sets the timeout duration for HTTP requests. */
        fun withTimeout(timeout: Duration)
        {
            this.timeout = timeout
        }

        internal fun init()
        {
            if (configuration?.developmentMode == true)
            {
                println("Pakku is running in development mode")
            }
            else
            {
                requireNotNull(configuration?.curseForgeApiKey) { "curseForgeApiKey must be specified" }
                requireNotNull(configuration?.userAgent) { "userAgent must be specified" }
            }
        }
    }

    private var configuration: Configuration? = null

    @Throws(IllegalStateException::class)
    internal fun configure(block: Configuration.() -> Unit)
    {
        if (configuration == null)
        {
            configuration = Configuration().apply(block)
            configuration!!.init()
        }
    }

    /** Indicates whether development mode is enabled. */
    internal val developmentMode: Boolean
        get() = configuration?.developmentMode ?: false

    /** The CurseForge API key used for authentication. */
    internal val curseForgeApiKey: String?
        get() = configuration?.curseForgeApiKey

    /** The user agent used for HTTP requests. */
    internal val userAgent: String?
        get() = configuration?.userAgent

    /** The timeout duration for HTTP requests. */
    internal val timeout: Duration
        get() = configuration?.timeout ?: 3.minutes
}

/** Initializes Pakku with the provided configuration. */
fun pakku(block: PakkuApi.Configuration.() -> Unit)
{
    PakkuApi.configure(block)
}
