@file:Suppress("unused")

package teksturepako.pakku.api.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToStream
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files

@Serializable
data class PakkuLock(
    /**
     * The pack name.
     */
    @SerialName("pack_name") var packName: String = "",

    /**
     * This pack's Minecraft version
     */
    @SerialName("mc_versions") var mcVersions: MutableList<String> = mutableListOf(),

    /**
     * The mod loader
     */
    var loaders: MutableList<String> = mutableListOf(),

    /**
     * List of projects.
     */
    val projects: MutableList<Project> = mutableListOf()
)
{
    internal companion object
    {
        @OptIn(ExperimentalSerializationApi::class)
        internal suspend fun <T> handle(block: (data: PakkuLock) -> T)
        {
            File("pakku-lock.json").also { pakkuLock ->
                // Try to get read json data first
                // If pakkuLock does not exist, create a new one
                val data: PakkuLock = if (pakkuLock.exists()) try
                {
                    json.decodeFromString<PakkuLock>(readFile(pakkuLock, Charsets.UTF_8))
                } catch (e: Exception)
                {
                    debug { e.printStackTrace() }
                    return
                }
                else
                {
                    PakkuLock()
                }

                // Handle data manipulation
                block(data)

                // Override file text
                FileOutputStream(pakkuLock).close()

                // Write to file
                FileOutputStream(pakkuLock).use { file ->
                    json.encodeToStream(data, file)
                }
            }
        }

        internal suspend fun <T> get(block: (data: PakkuLock) -> T): T
        {
            File("pakku-lock.json").also { pakkuLock ->
                // Try to get read json data first
                val data: PakkuLock = try
                {
                    json.decodeFromString(readFile(pakkuLock, Charsets.UTF_8))
                } catch (e: Exception)
                {
                    debug { e.printStackTrace() }
                    PakkuLock()
                }

                // Get data
                return block(data)
            }
        }

        @Throws(IOException::class)
        private suspend fun readFile(file: File, encoding: Charset): String
        {
            val encoded = withContext(Dispatchers.IO) {
                Files.readAllBytes(file.toPath())
            }
            return String(encoded, encoding)
        }


        suspend fun addProject(project: Project): Boolean?
        {
            var added: Boolean? = false
            handle { data ->
                added = if (data.projects.any { project isAlmostTheSameAs it })
                {
                    debug { println("Could not add ${project.name}") }
                    null
                } else
                {
                    // Add project
                    data.projects.add(project)
                    true
                }
                // Sort alphabetically
                data.projects.sortBy { it.slug.values.first() }
            }
            return added
        }

        suspend fun updateProject(project: Project): Boolean?
        {
            var updated: Boolean? = false
            handle { data ->
                // Override old project
                data.projects.removeIf { it isAlmostTheSameAs project }.also {
                    // Print error, if it could not remove
                    updated = if (!it)
                    {
                        debug { println("Could not update ${project.name}") }
                        null
                    } else
                    {
                        // Add project
                        data.projects.add(project)
                        true
                    }
                }
                // Sort alphabetically
                data.projects.sortBy { it.slug.values.first() }
            }
            return updated
        }

        suspend fun removeProject(project: Project): Boolean?
        {
            var removed: Boolean? = false
            handle { data ->
                // Remove project
                data.projects.removeIf { it isAlmostTheSameAs project }.also { success ->
                    removed = if (!success)
                    {
                        debug { println("Could not remove ${project.name}") }
                        null
                    } else
                    {
                        true
                    }
                }
            }
            return removed
        }


        suspend fun removeProjectByPakkuId(pakkuId: String): Boolean?
        {
            var removed: Boolean? = false
            handle { data ->
                // Remove project
                data.projects.removeIf { pakkuId == it.pakkuId }.also { success ->
                    removed = if (!success)
                    {
                        debug { println("Could not remove project from pakku id ($pakkuId)") }
                        null
                    } else
                    {
                        true
                    }
                }
            }
            return removed
        }

        suspend fun addPakkuLink(pakkuId: String, project: Project) = handle { data ->
            data.projects.find { it isAlmostTheSameAs project }?.pakkuLinks?.add(pakkuId)
        }

        suspend fun removePakkuLink(pakkuId: String) = handle { data ->
            data.projects.map { it.pakkuLinks.remove(pakkuId) }
        }

        suspend fun getProjectByPakkuId(pakkuId: String): Project? = get { data ->
            data.projects.find { pakkuId == it.pakkuId }
        }

        suspend fun getLinkedProjects(pakkuId: String): List<Project> = get { data ->
            data.projects.filter { pakkuId in it.pakkuLinks }
        }

        suspend fun getLinkedProjects(pakkuId: String, ignore: Project): List<Project> = get { data ->
            data.projects.filter { pakkuId in it.pakkuLinks && !ignore.isAlmostTheSameAs(it) }
        }


        suspend fun setPackName(packName: String) = handle {
            data -> data.packName = packName
        }

        suspend fun setMcVersion(vararg mcVersion: String) = handle { data ->
            data.mcVersions.clear(); data.mcVersions.addAll(mcVersion)
        }

        suspend fun setMcVersion(mcVersions: List<String>) = handle { data ->
            data.mcVersions.clear(); data.mcVersions.addAll(mcVersions)
        }

        suspend fun setModLoader(vararg loader: String) = handle { data ->
            data.loaders.clear(); data.loaders.addAll(loader)
        }

        suspend fun setModLoader(loaders: List<String>) = handle { data ->
            data.loaders.clear(); data.loaders.addAll(loaders)
        }


        suspend fun isProjectAdded(project: Project): Boolean = get { data ->
            data.projects.any { it isAlmostTheSameAs project }
        }

        suspend fun getProject(input: String): Project? = get { data ->
            data.projects.find { project ->
                input in project
            }
        }

        suspend fun getProject(project: Project): Project? = get { data ->
            data.projects.find {
                it isAlmostTheSameAs project
            }
        }

        suspend fun getAllProjects(): List<Project> = get { data ->
            data.projects
        }

        suspend fun getPackName() = get { data -> data.packName }
        suspend fun getMcVersions() = get { data -> data.mcVersions }
        suspend fun getLoaders() = get { data -> data.loaders }
    }
}