package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.io.*
import java.nio.file.Path as NioPath
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * A local config file (`pakku-local.json`) contains local modifications
 * that extend the base pakku.json configuration without modifying the upstream.
 *
 * This file is intended for managing divergent modpack forks.
 */
@Serializable
data class LocalConfig(
    /** The version of the local modpack fork. */
    private var version: String = "",

    /** The description of the local fork. */
    private var description: String = "",

    /** List of project slugs that are local-only additions (not in parent). */
    @SerialName("local_only") val localOnly: MutableList<String> = mutableListOf(),

    /** A mutable map of project slugs to their local configurations. */
    val projects: MutableMap<String, LocalProjectConfig> = mutableMapOf()
)
{
    fun setVersion(version: String) { this.version = version }
    fun setDescription(description: String) { this.description = description }

    fun getVersion() = this.version
    fun getDescription() = this.description

    fun addLocalOnly(projectSlug: String) {
        if (projectSlug !in localOnly) {
            localOnly.add(projectSlug)
        }
    }

    fun removeLocalOnly(projectSlug: String) {
        localOnly.remove(projectSlug)
    }

    fun isLocalOnly(projectSlug: String): Boolean = projectSlug in localOnly

    companion object
    {
        const val FILE_NAME = "pakku-local.json"

        fun exists(): Boolean = Path(workingPath, FILE_NAME).exists()
        fun existsAt(path: NioPath): Boolean = path.exists()

        fun readOrNull(): LocalConfig? = decodeToResult<LocalConfig>("$workingPath/$FILE_NAME").getOrNull()

        fun readOrNew(): LocalConfig = decodeOrNew(LocalConfig(), "$workingPath/$FILE_NAME")
    }

    suspend fun write() = writeToFile(this, "$workingPath/$FILE_NAME", overrideText = true, format = json)
}

/**
 * Local project configuration that overrides or extends base project settings.
 */
@Serializable
data class LocalProjectConfig(
    var type: String? = null,
    var side: String? = null,
    @SerialName("update_strategy") var updateStrategy: String? = null,
    @SerialName("redistributable") var redistributable: Boolean? = null,
    var subpath: String? = null,
    var aliases: MutableSet<String>? = null,
    var export: Boolean? = null
)
