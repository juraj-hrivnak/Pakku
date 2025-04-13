@file:Suppress("unused")

package teksturepako.pakku.api.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.platforms.*
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.containProject
import teksturepako.pakku.api.projects.inheritPropertiesFrom
import teksturepako.pakku.debug
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.writeToFile
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * A lock file (`pakku-lock.json`) is an automatically generated file used by Pakku
 * to define all properties of a modpack needed for its development.
 *
 * This file is not intended to be modified manually.
 */
@Serializable
data class LockFile(
    /** Targeted platform of the modpack. */
    private var target: String? = null,

    /** Minecraft versions supported by the modpack. */
    @SerialName("mc_versions") private var mcVersions: MutableList<String> = mutableListOf(),

    /**
     *  Mutable map of _loader names_ to _loader versions_ supported by the modpack.
     *  _loader names_ will always be formated to lowercase.
     */
    private val loaders: MutableMap<String, String> = mutableMapOf(),

    /** List of projects included in the modpack. */
    private var projects: MutableList<Project> = mutableListOf(),

    /** The version of the LockFile. */
    @SerialName("lockfile_version") @Required private val lockFileVersion: Int = 1,
)
{
    // -- MC VERSIONS --

    fun setMcVersions(mcVersions: Collection<String>)
    {
        this.mcVersions.clear()
        this.mcVersions.addAll(mcVersions)
    }

    fun getMcVersions() = this.mcVersions
    fun getFirstMcVersion() = this.mcVersions.firstOrNull()

    // -- LOADERS --

    fun addLoader(loaderName: String, loaderVersion: String)
    {
        this.loaders[loaderName] = loaderVersion
    }

    fun setLoader(loaderName: String, loaderVersion: String)
    {
        this.loaders[loaderName] = loaderVersion
    }

    fun setLoaders(loaders: Map<String, String>)
    {
        this.loaders.clear()
        this.loaders.putAll(loaders)
    }

    fun getLoaders() = this.loaders.keys.toList().map { it.lowercase() }
    fun getLoadersWithVersions() = this.loaders.toList().map { it.first.lowercase() to it.second }

    // -- TARGET --

    fun setTarget(target: String)
    {
        this.target = target
    }

    data class TargetNotFound(val target: String? = null) : ActionError()
    {
        override val rawMessage = if (target != null) "Target '$target' not found" else "Target not found"
    }

    fun getPlatforms(): Result<List<Platform>, ActionError> = if (target != null) when (target!!.lowercase())
    {
        "curseforge" -> Ok(listOf(CurseForge))
        "modrinth" -> Ok(listOf(Modrinth))
        "multiplatform" -> Ok(Multiplatform.platforms)
        else -> Err(TargetNotFound(target))
    }
    else Err(TargetNotFound())

    fun getProjectProvider(): Result<Provider, ActionError> = if (target != null) when (target!!.lowercase())
    {
        "curseforge" -> Ok(CurseForge)
        "modrinth" -> Ok(Modrinth)
        "multiplatform" -> Ok(Multiplatform)
        else -> Err(TargetNotFound(target))
    }
    else Err(TargetNotFound())

    // -- PROJECTS --

    fun add(project: Project): Boolean?
    {
        val added: Boolean? = if (projects containProject project)
        {
            debug { println("Could not add ${project.name}") }
            null
        }
        else
        {
            // Add project
            this.projects.add(project)
            true
        }
        // Sort alphabetically by name
        this.projects.sortBy { it.name.values.firstOrNull() }

        return added
    }

    fun addAll(projects: Collection<Project>): Boolean
    {
        var added: Boolean
        this.projects.addAll(projects).also {
            added = it
        }
        this.projects.sortBy { it.name.values.firstOrNull() }

        return added
    }

    fun isProjectAdded(project: Project): Boolean = this.projects.any { it isAlmostTheSameAs project }

    fun update(project: Project): Boolean?
    {
        var updated: Boolean?

        // Override old project
        this.projects.removeIf { it isAlmostTheSameAs project }.also {
            // Print error, if it could not remove
            updated = if (!it)
            {
                debug { println("Could not update ${project.name}") }
                null
            }
            else
            {
                // Add project
                this.projects.add(project)
                true
            }
        }
        // Sort alphabetically
        this.projects.sortBy { it.name.values.firstOrNull() }

        return updated
    }

    fun updateAll(projects: Collection<Project>): Boolean
    {
        var updated: Boolean
        this.projects.removeAll(projects).also {
            updated = it
        }
        this.projects.addAll(projects).also {
            updated = it
        }
        this.projects.sortBy { it.name.values.firstOrNull() }

        return updated
    }

    fun remove(project: Project): Boolean?
    {
        var removed: Boolean?
        // Remove project
        this.projects.removeIf { it isAlmostTheSameAs project }.also { success ->
            removed = if (!success)
            {
                debug { println("Could not remove ${project.name}") }
                null
            }
            else true
        }

        return removed
    }

    fun removeAllProjects() = this.projects.clear()

    fun removeProjectByPakkuId(pakkuId: String): Boolean?
    {
        var removed: Boolean?
        // Remove project
        this.projects.removeIf { pakkuId == it.pakkuId }.also { success ->
            removed = if (!success)
            {
                debug { println("Could not remove project from pakku id ($pakkuId)") }
                null
            }
            else true
        }
        return removed
    }

    fun getProject(input: String): Project? = this.projects.find { project -> input in project }

    fun getProject(project: Project): Project? = this.projects.find { it isAlmostTheSameAs project }

    fun getAllProjects(): List<Project> = this.projects

    fun getProjectByPakkuId(pakkuId: String): Project? = this.projects.find { pakkuId == it.pakkuId }

    // -- DEPENDENCIES --

    fun addPakkuLink(pakkuId: String, project: Project) =
        this.projects.find { it isAlmostTheSameAs project }?.pakkuLinks?.add(pakkuId)

    fun getLinkedProjects(pakkuId: String): List<Project> = this.projects.filter { pakkuId in it.pakkuLinks }

    fun getLinkedProjects(pakkuId: String, ignore: Project): List<Project> =
        this.projects.filter { pakkuId in it.pakkuLinks && !ignore.isAlmostTheSameAs(it) }

    fun removePakkuLinkFromAllProjects(pakkuId: String) = this.projects.map { it.pakkuLinks.remove(pakkuId) }

    // -- DEPENDENTS --

    fun linkProjectToDependents(project: Project)
    {
        for (dependentProject in this.projects)
        {
            x@ for (file in dependentProject.files)
            {
                val dependencyIds = file.requiredDependencies ?: continue@x
                if (project.id.values.any { id -> id in dependencyIds } && project.pakkuId !in dependentProject.pakkuLinks)
                {
                    dependentProject.pakkuLinks.add(project.pakkuId!!)
                }
            }
        }
    }

    // -- CONFIG INHERITANCE --

    fun inheritConfig(configFile: ConfigFile?)
    {
        this.projects = this.projects.inheritPropertiesFrom(configFile)
    }

    // -- FILE I/O --

    companion object
    {
        const val FILE_NAME = "pakku-lock.json"

        fun exists(): Boolean = Path(workingPath, FILE_NAME).exists()
        fun existsAt(path: Path): Boolean = path.exists()

        /** Reads [LockFile] and parses it, or returns a new [LockFile]. */
        fun readOrNew(): LockFile = decodeOrNew<LockFile>(LockFile(), "$workingPath/$FILE_NAME")
            .also { it.inheritConfig(ConfigFile.readOrNull()) }

        /** Reads [LockFile] and parses it to a [Result]. */
        suspend fun readToResult(): Result<LockFile, ActionError> =
            decodeToResult<LockFile>(Path("$workingPath/$FILE_NAME"))
                .onSuccess { it.inheritConfig(ConfigFile.readOrNull()) }

        /** Reads [LockFile] from a specified [path] and parses it to a [Result]. */
        suspend fun readToResultFrom(path: Path): Result<LockFile, ActionError> =
            decodeToResult<LockFile>(path)
                .onSuccess { it.inheritConfig(ConfigFile.readOrNull()) }
    }

    suspend fun write() = writeToFile(this, "$workingPath/$FILE_NAME", overrideText = true)
}
