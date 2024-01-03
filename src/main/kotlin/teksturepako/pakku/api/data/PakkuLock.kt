@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.writeToFile
import java.io.File

/**
 * A PakkuLock is used to define the properties of a Minecraft modpack.
 *
 * @property packName The name of the mod pack.
 * @property mcVersions The Minecraft versions supported by the mod pack.
 * @property loaders The mod loaders used by the mod pack.
 * @property projects A list of associated projects with the mod pack.
 */
@Serializable
data class PakkuLock(
    @SerialName("pack_name") private var packName: String = "",
    @SerialName("mc_versions") private var mcVersions: MutableList<String> = mutableListOf(),
    private var loaders: MutableList<String> = mutableListOf(),
    private val projects: MutableList<Project> = mutableListOf()
)
{
    fun add(project: Project): Boolean?
    {
        val added: Boolean? = if (projects.any { project isAlmostTheSameAs it })
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

    fun removePakkuLink(pakkuId: String) = this.projects.map { it.pakkuLinks.remove(pakkuId) }


    fun setPackName(packName: String)
    {
        this.packName = packName
    }

    fun setMcVersion(vararg mcVersion: String)
    {
        this.mcVersions.clear()
        this.mcVersions.addAll(mcVersion)
    }

    fun setMcVersion(mcVersions: List<String>)
    {
        this.mcVersions.clear()
        this.mcVersions.addAll(mcVersions)
    }

    fun setModLoader(vararg loader: String)
    {
        this.loaders.clear()
        this.loaders.addAll(loader)
    }

    fun setModLoader(loaders: List<String>)
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


    companion object
    {
        private const val PAKKU_FILE = "pakku-lock.json"

        suspend fun readOrNew(): PakkuLock = readFileOrNull(File(PAKKU_FILE))?.let {
            runCatching { json.decodeFromString<PakkuLock>(it) }.getOrElse { PakkuLock() }
        } ?: PakkuLock()
    }

    fun write() = writeToFile(this, File(PAKKU_FILE), overrideText = true)
}