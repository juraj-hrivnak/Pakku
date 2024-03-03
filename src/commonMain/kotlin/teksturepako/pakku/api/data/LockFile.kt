@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.containsProject
import teksturepako.pakku.debug
import teksturepako.pakku.io.decodeOrNew
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.writeToFile

/**
 * A LockFile is used to define all properties of a Pakku modpack.
 *
 * @property name The name of the mod pack.
 * @property mcVersions The Minecraft versions supported by the mod pack.
 * @property loaders The mod loaders used by the mod pack.
 * @property projects A list of associated projects with the mod pack.
 */
@Serializable
data class LockFile(
    private var name: String = "",
    private var target: String? = null,
    @SerialName("mc_versions") private var mcVersions: MutableList<String> = mutableListOf(),
    private var loaders: MutableList<String> = mutableListOf(),
    private val projects: MutableList<Project> = mutableListOf(),

)
{
    // -- PACK --

    fun setName(name: String)
    {
        this.name = name
    }

    fun setTarget(target: String)
    {
        this.target = target
    }

    fun setMcVersions(mcVersions: Collection<String>)
    {
        this.mcVersions.clear()
        this.mcVersions.addAll(mcVersions)
    }

    fun setLoaders(loaders: Collection<String>)
    {
        this.loaders.clear()
        this.loaders.addAll(loaders)
    }

    fun getName() = this.name
    fun getMcVersions() = this.mcVersions
    fun getLoaders() = this.loaders

    // -- TARGET --

    fun getPlatforms(): Result<List<Platform>> = if (target != null) when (target!!.lowercase())
    {
        "curseforge" -> Result.success(listOf(CurseForge))
        "modrinth" -> Result.success(listOf(Modrinth))
        "multiplatform" -> Result.success(Multiplatform.platforms)
        else -> Result.failure(PakkuException("Target '$target' not found"))
    } else Result.failure(PakkuException("Target not found"))

    fun getProjectProvider(): Result<IProjectProvider> = if (target != null) when (target!!.lowercase())
    {
        "curseforge" -> Result.success(CurseForge)
        "modrinth" -> Result.success(Modrinth)
        "multiplatform" -> Result.success(Multiplatform)
        else -> Result.failure(PakkuException("Target (project provider) '$target' not found"))
    } else Result.failure(PakkuException("Target (project provider) not found"))

    // -- PROJECTS --

    fun add(project: Project): Boolean?
    {
        val added: Boolean? = if (projects containsProject project)
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
        // Sort alphabetically
        this.projects.sortBy { it.slug.values.first() }

        return added
    }

    fun addAll(projects: Collection<Project>): Boolean
    {
        var added: Boolean
        this.projects.addAll(projects).also {
            added = it
        }
        this.projects.sortBy { it.slug.values.first() }

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
        this.projects.sortBy { it.slug.values.first() }

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
        for (dependent in this.projects)
        {
            x@ for (file in dependent.files)
            {
                val deps = file.requiredDependencies ?: continue@x
                if (project.id.values.any { it in deps } && project.pakkuId !in dependent.pakkuLinks)
                {
                    dependent.pakkuLinks.add(project.pakkuId!!)
                }
            }
        }
    }

    // -- FILE I/O --

    private val configFile = runBlocking { ConfigFile.readToResult().getOrNull() }

    init
    {
        // Inherit name
        configFile?.name?.let { name = it }
    }

    companion object
    {
        const val FILE_NAME = "pakku-lock.json"

        suspend fun exists(): Boolean = readFileOrNull(FILE_NAME) != null

        /** Reads [LockFile] and parses it, or returns a new [LockFile]. */
        suspend fun readOrNew(): LockFile = decodeOrNew(LockFile(), FILE_NAME)

        /**
         * Reads [LockFile] and parses it, or returns an exception.
         * Use [Result.fold] to map it's [success][Result.success] or [failure][Result.failure] values.
         */
        suspend fun readToResult(): Result<LockFile> = decodeToResult(LockFile(), FILE_NAME)

        suspend fun readToResultFrom(path: String): Result<LockFile> = decodeToResult(LockFile(), path)
    }

    suspend fun write() = writeToFile(this, FILE_NAME, overrideText = true)
}
