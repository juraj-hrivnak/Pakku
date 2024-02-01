@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.containsProject
import teksturepako.pakku.api.projects.removeIf
import teksturepako.pakku.debug
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.writeToFile

/**
 * A PakkuLock is used to define all properties of a Pakku modpack.
 *
 * @property packName The name of the mod pack.
 * @property mcVersions The Minecraft versions supported by the mod pack.
 * @property loaders The mod loaders used by the mod pack.
 * @property projects A list of associated projects with the mod pack.
 */
@Serializable
data class PakkuLock(
    @SerialName("name") private var packName: String = "",
    @SerialName("target") private var platformTarget: String? = null,
    @SerialName("mc_versions") private var mcVersions: MutableList<String> = mutableListOf(),
    private var loaders: MutableList<String> = mutableListOf(),
    private val projects: MutableList<Project> = mutableListOf()
)
{
    fun add(project: Project): Boolean?
    {
        val added: Boolean? = if (projects containsProject project)
        {
            debug { println("Could not add ${project.name}") }
            null
        } else
        {
            // Add project
            this.projects.add(project)
            true
        }
        // Sort alphabetically
        this.projects.sortBy { it.slug.values.first() }

        return added
    }

    fun addAll(projects: List<Project>): Boolean
    {
        var added: Boolean
        this.projects.addAll(projects).also {
            added = it
        }
        this.projects.sortBy { it.slug.values.first() }

        return added
    }

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
            } else
            {
                // Add project
                this.projects.add(project)
                true
            }
        }
        // Sort alphabetically
        this.projects.sortBy { it.slug.values.first() }

        return updated
    }

    fun updateAll(projects: List<Project>): Boolean
    {
        var updated: Boolean
        this.projects.removeAll(projects).also {
            updated = it
        }
        this.projects.addAll(projects).also {
            updated = it
        }
        this.projects.sortBy { it.slug.values.first() }

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
            } else
            {
                true
            }
        }

        return removed
    }

    fun removeAll() = this.projects.clear()

    fun removeProjectByPakkuId(pakkuId: String): Boolean?
    {
        var removed: Boolean?
        // Remove project
        this.projects.removeIf { pakkuId == it.pakkuId }.also { success ->
            removed = if (!success)
            {
                debug { println("Could not remove project from pakku id ($pakkuId)") }
                null
            } else
            {
                true
            }
        }
        return removed
    }

    fun addPakkuLink(pakkuId: String, project: Project) =
        this.projects.find { it isAlmostTheSameAs project }?.pakkuLinks?.add(pakkuId)

    fun removePakkuLinkFromAllProjects(pakkuId: String) = this.projects.map { it.pakkuLinks.remove(pakkuId) }


    fun setPackName(packName: String)
    {
        this.packName = packName
    }

    fun setPlatformTarget(target: String)
    {
        this.platformTarget = target
    }

    fun setMcVersions(mcVersions: List<String>)
    {
        this.mcVersions.clear()
        this.mcVersions.addAll(mcVersions)
    }

    fun setModLoaders(loaders: List<String>)
    {
        this.loaders.clear()
        this.loaders.addAll(loaders)
    }


    fun isProjectAdded(project: Project): Boolean = this.projects.any { it isAlmostTheSameAs project }

    fun getProject(input: String): Project? = this.projects.find { project -> input in project }

    fun getProject(project: Project): Project? = this.projects.find { it isAlmostTheSameAs project }

    fun getAllProjects(): List<Project> = this.projects

    fun getProjectByPakkuId(pakkuId: String): Project? = this.projects.find { pakkuId == it.pakkuId }

    fun getLinkedProjects(pakkuId: String): List<Project> = this.projects.filter { pakkuId in it.pakkuLinks }

    fun getLinkedProjects(pakkuId: String, ignore: Project): List<Project> =
        this.projects.filter { pakkuId in it.pakkuLinks && !ignore.isAlmostTheSameAs(it) }

    fun getPackName() = this.packName
    fun getMcVersions() = this.mcVersions
    fun getLoaders() = this.loaders


    fun linkProjectToDependants(project: Project)
    {
        for (dependant in this.projects)
        {
            x@ for (file in dependant.files)
            {
                val deps = file.requiredDependencies ?: continue@x
                if (project.id.values.any { it in deps } && project.pakkuId !in dependant.pakkuLinks)
                {
                    dependant.pakkuLinks.add(project.pakkuId!!)
                }
            }
        }
    }


    fun getPlatforms(): Result<List<Platform>> = if (platformTarget != null) when (platformTarget!!.lowercase())
    {
        "curseforge" -> Result.success(listOf(CurseForge))
        "modrinth" -> Result.success(listOf(Modrinth))
        "multiplatform" -> Result.success(Multiplatform.platforms)
        else -> Result.failure(PakkuException("Target '$platformTarget' not found"))
    } else Result.failure(PakkuException("Target not found"))

    fun getProjectProvider(): Result<IProjectProvider> = if (platformTarget != null) when (platformTarget!!.lowercase())
    {
        "curseforge" -> Result.success(CurseForge)
        "modrinth" -> Result.success(Modrinth)
        "multiplatform" -> Result.success(Multiplatform)
        else -> Result.failure(PakkuException("Target (project provider) '$platformTarget' not found"))
    } else Result.failure(PakkuException("Target (project provider) not found"))

    companion object
    {
        private const val FILE_NAME = "pakku-lock.json"

        /**
         * Reads [PakkuLock's][PakkuLock] [file][FILE_NAME] and parses it,
         * or returns a new [PakkuLock].
         */
        suspend fun readOrNew(): PakkuLock = readFileOrNull(FILE_NAME)?.let {
            runCatching { json.decodeFromString<PakkuLock>(it) }.getOrElse { PakkuLock() }
        } ?: PakkuLock()

        /**
         * Reads [PakkuLock's][PakkuLock] [file][FILE_NAME] and parses it, or returns an exception.
         * Use [Result.fold] to map it's [success][Result.success] or [failure][Result.failure] values.
         */
        suspend fun readToResult(): Result<PakkuLock> = readFileOrNull(FILE_NAME)?.let {
            runCatching { Result.success(json.decodeFromString<PakkuLock>(it)) }.getOrElse { exception ->
                Result.failure(PakkuException("Error occurred while reading '$FILE_NAME': ${exception.message}"))
            }
        } ?: Result.failure(PakkuException("Could not read '$FILE_NAME'"))
    }

    suspend fun write() = writeToFile(this, FILE_NAME, overrideText = true)
}
