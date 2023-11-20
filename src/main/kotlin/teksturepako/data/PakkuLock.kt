package teksturepako.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToStream
import teksturepako.debug
import teksturepako.projects.Project
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
    @SerialName("pack_name")
    var packName: String,

    /**
     * This pack's Minecraft version
     */
    @SerialName("mc_version")
    var mcVersion: String,

    /**
     * The mod loader
     */
    @SerialName("mod_loader")
    var modLoader: String,

    /**
     * List of projects.
     */
    val projects: MutableList<Project> = mutableListOf()
) {
    companion object
    {
        @OptIn(ExperimentalSerializationApi::class)
        suspend fun <T> handle(block: (data: PakkuLock) -> T)
        {
            File("pakku-lock.json").also { pakkuLock ->
                // Try to get read json data first
                val data: PakkuLock = try
                {
                    json.decodeFromString<PakkuLock>(readFile(pakkuLock, Charsets.UTF_8))
                }
                catch (e: Exception)
                {
                    debug { e.printStackTrace() }
                    return
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

        suspend fun <T> get(block: (data: PakkuLock) -> T): T
        {
            File("pakku-lock.json").also { pakkuLock ->
                // Try to get read json data first
                val data: PakkuLock = try
                {
                    json.decodeFromString(readFile(pakkuLock, Charsets.UTF_8))
                }
                catch (e: Exception)
                {
                    debug { e.printStackTrace() }
                    PakkuLock("", "", "", mutableListOf())
                }

                // Get data
                return block(data)
            }
        }

        suspend fun addProject(vararg projects: Project) = handle { data ->
            projects.forEach { project ->
                // Override old project
                data.projects.removeIf { it.slug == project.slug }.also {
                    // Print error, if it could not remove
                    if (!it) debug { println("Could not remove ${project.name}") }
                }
                // Add project
                data.projects.add(project)
            }

            // Sort alphabetically
            data.projects.sortBy { it.slug }
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
        suspend fun setMcVersion(mcVersion: String) = handle { data -> data.mcVersion = mcVersion }
        suspend fun setModLoader(modLoader: String) = handle { data -> data.modLoader = modLoader }

        suspend fun getPackName() = get { data -> data.packName }
        suspend fun getMcVersion() = get { data -> data.mcVersion }
        suspend fun getModLoader() = get { data -> data.modLoader }

        @Throws(IOException::class)
        suspend fun readFile(file: File, encoding: Charset): String
        {
            val encoded = withContext(Dispatchers.IO) {
                Files.readAllBytes(file.toPath())
            }
            return String(encoded, encoding)
        }
    }
}