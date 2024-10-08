@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.overrides.filterOverrides
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.readPathTextOrNull
import teksturepako.pakku.io.writeToFile

/**
 * A config file (`pakku.json`) is a file used by the user to configure properties needed for modpack export.
 */
@Serializable
data class ConfigFile(
    /** The name of the modpack. */
    private var name: String = "",

    /** The version of the modpack. */
    private var version: String = "",

    /** The description of the modpack. */
    private var description: String = "",

    /** The author of the modpack. */
    private var author: String = "",

    /** A mutable list of overrides packed up with the modpack. */
    private val overrides: MutableList<String> = mutableListOf(),

    /** A mutable list of server overrides packed up with the modpack. */
    @SerialName("server_overrides") private val serverOverrides: MutableList<String> = mutableListOf(),

    /** A mutable list of client overrides packed up with the modpack. */
    @SerialName("client_overrides") private val clientOverrides: MutableList<String> = mutableListOf(),
    /**  A map of project types to their respective paths. */
    val paths: MutableMap<String, String> = mutableMapOf(),

    /** A mutable map of _project slugs, names, IDs or filenames_ to _project configs_. */
    val projects: MutableMap<String, ProjectConfig> = mutableMapOf()
)
{
    // -- PACK --

    fun setName(name: String)
    {
        this.name = name
    }

    fun setVersion(version: String)
    {
        this.version = version
    }

    fun setDescription(description: String)
    {
        this.description = description
    }

    fun setAuthor(author: String)
    {
        this.author = author
    }

    fun getName() = this.name
    fun getVersion() = this.version
    fun getDescription() = this.description
    fun getAuthor() = this.author

    // -- OVERRIDES --

    fun addOverride(override: String)
    {
        this.overrides.add(override)
    }

    fun addOverrides(vararg overrides: String)
    {
        this.overrides.addAll(overrides)
    }

    fun addOverrides(overrides: Collection<String>)
    {
        this.overrides.addAll(overrides)
    }

    fun removeOverride(override: String)
    {
        this.overrides.remove(override)
    }

    fun removeAllOverrides()
    {
        this.overrides.clear()
    }

    fun getAllOverrides(): List<String> = filterOverrides(this.overrides)
    fun getAllServerOverrides(): List<String> = filterOverrides(this.serverOverrides)
    fun getAllClientOverrides(): List<String> = filterOverrides(this.clientOverrides)

    // -- PROJECTS --

    @Serializable
    data class ProjectConfig(
        var type: ProjectType? = null,
        var side: ProjectSide? = null,
        @SerialName("update_strategy") var updateStrategy: UpdateStrategy? = null,
        @SerialName("redistributable") var redistributable: Boolean? = null,
        var subpath: String? = null,
        var aliases: MutableSet<String>? = null
    )

    // -- FILE I/O --

    companion object
    {
        const val FILE_NAME = "pakku.json"

        fun exists(): Boolean = readPathTextOrNull("$workingPath/$FILE_NAME") != null

        fun readOrNew(): ConfigFile = decodeOrNew(ConfigFile(), "$workingPath/$FILE_NAME")

        fun readOrNull() = decodeToResult<ConfigFile>("$workingPath/$FILE_NAME").getOrNull()

        /**
         * Reads [LockFile] and parses it, or returns an exception.
         * Use [Result.fold] to map it's [success][Result.success] or [failure][Result.failure] values.
         */
        fun readToResult(): Result<ConfigFile> = decodeToResult("$workingPath/$FILE_NAME")

        fun readToResultFrom(path: String): Result<ConfigFile> = decodeToResult(path)
    }

    suspend fun write() = writeToFile(this, "$workingPath/$FILE_NAME", overrideText = true, format = json)
}