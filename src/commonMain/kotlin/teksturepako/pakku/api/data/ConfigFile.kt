@file:Suppress("unused")

package teksturepako.pakku.api.data

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.io.*
import java.nio.file.Path
import kotlin.io.path.Path

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

    suspend fun getAllOverrides(): List<Result<String, ActionError>> =
        this.overrides.expandWithGlob(Path(workingPath)).map { filterPath(it) }

    suspend fun getAllServerOverrides(): List<Result<String, ActionError>> =
        this.serverOverrides.expandWithGlob(Path(workingPath)).map { filterPath(it) }

    suspend fun getAllClientOverrides(): List<Result<String, ActionError>> =
        this.clientOverrides.expandWithGlob(Path(workingPath)).map { filterPath(it) }

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
         * Use [Result.fold] to map it's success and failure values.
         */
        suspend fun readToResult(): Result<ConfigFile, ActionError> =
            decodeToResult<ConfigFile>(Path(workingPath, FILE_NAME))

        suspend fun readToResultFrom(path: Path): Result<ConfigFile, ActionError> =
            decodeToResult<ConfigFile>(path)
    }

    suspend fun write() = writeToFile(this, "$workingPath/$FILE_NAME", overrideText = true, format = json)
}