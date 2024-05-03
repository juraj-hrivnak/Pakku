@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.writeToFile

@Serializable
data class ConfigFile(
    private var name: String = "",
    private var version: String = "",
    private var description: String = "",
    private var author: String = "",
    private val overrides: MutableList<String> = mutableListOf(),
    @SerialName("server_overrides")
    private val serverOverrides: MutableList<String> = mutableListOf(),
    @SerialName("client_overrides") private val clientOverrides: MutableList<String> = mutableListOf(),
    private val projects: MutableMap<String, ProjectConfig> = mutableMapOf()
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

    fun addAllOverrides(overrides: Collection<String>)
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

    fun getAllOverrides(): List<String> = Overrides.filter(this.overrides)

    fun getAllServerOverrides(): List<String> = Overrides.filter(this.serverOverrides)

    fun getAllClientOverrides(): List<String> = Overrides.filter(this.clientOverrides)

    // -- PROJECTS --

    fun getProjects() = this.projects

    @Serializable
    data class ProjectConfig(
        var side: ProjectSide?,
        @SerialName("update_strategy") var updateStrategy: UpdateStrategy?,
        @SerialName("redistributable") var redistributable: Boolean?
    )

    // -- FILE I/O --

    companion object
    {
        const val FILE_NAME = "pakku.json"

        suspend fun exists(): Boolean = readFileOrNull("$workingPath/$FILE_NAME") != null

        suspend fun readOrNew(): ConfigFile = decodeOrNew(ConfigFile(), "$workingPath/$FILE_NAME")

        suspend fun readOrNull() = decodeToResult<ConfigFile>("$workingPath/$FILE_NAME").getOrNull()

        /**
         * Reads [LockFile] and parses it, or returns an exception.
         * Use [Result.fold] to map it's [success][Result.success] or [failure][Result.failure] values.
         */
        suspend fun readToResult(): Result<ConfigFile> = decodeToResult("$workingPath/$FILE_NAME")
    }

    suspend fun write() = writeToFile(this, "$workingPath/$FILE_NAME", overrideText = true, format = jsonEncodeDefaults)
}