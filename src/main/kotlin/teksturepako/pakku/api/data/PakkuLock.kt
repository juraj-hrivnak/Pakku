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
    companion object
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


        suspend fun addProject(vararg projects: Project) = handle { data ->
            projects.forEach { project ->
                if (project.slug.values.any { slug -> slug in data.projects.flatMap { it.slug.values } })
                {
                    debug { println("Could not add ${project.name}") }
                } else
                {
                    // Add project
                    data.projects.add(project)

                    // Sort alphabetically
                    data.projects.sortBy { it.slug.values.first() }
                }
            }
        }

        suspend fun updateProject(vararg projects: Project) = handle { data ->
            projects.forEach { project ->
                // Override old project
                data.projects.removeIf { it.slug == project.slug }.also {
                    // Print error, if it could not remove
                    if (!it)
                    {
                        debug { println("Could not update ${project.name}") }
                        return@handle
                    }
                }
                // Add project
                data.projects.add(project)
            }

            // Sort alphabetically
            data.projects.sortBy { it.slug.values.first() }
        }

        suspend fun removeProject(vararg projects: Project) = handle { data ->
            projects.forEach { project ->
                // Remove project
                data.projects.removeIf { it.slug == project.slug }.also {
                    if (!it) debug { println("Could not remove ${project.name}") }
                }
            }
        }


        suspend fun setPackName(packName: String) = handle { data -> data.packName = packName }

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


        suspend fun isProjectAdded(project: Project): Boolean
        {
            val projects = get { data ->
                data.projects
            }

            return (project.slug.values.any { slug -> slug in projects.flatMap { it.slug.values } }
                || project.id.values.any { id -> id in projects.flatMap { it.id.values }})
        }

        suspend fun getAllProjects(): List<Project> = get { data ->
            data.projects
        }

        suspend fun getPackName() = get { data -> data.packName }
        suspend fun getMcVersions() = get { data -> data.mcVersions }
        suspend fun getLoaders() = get { data -> data.loaders }
    }
}