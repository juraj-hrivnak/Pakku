package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.platforms.Multiplatform.platforms
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

/**
 * Platform is a site containing projects.
 */
abstract class Platform : Http()
{
    /**
     * Platform name.
     */
    abstract val name: String

    /**
     * Snake case version of the name.
     */
    abstract val serialName: String

    /**
     * The API URL address of this platform.
     */
    abstract val apiUrl: String

    /**
     * Version of the API.
     */
    abstract val apiVersion: Int

    suspend fun requestProjectBody(input: String): String?
    {
        return this.requestBody("$apiUrl/v$apiVersion/$input")
    }

    suspend fun requestProject(input: String): Project?
    {
        return requestProjectFromId(input) ?: requestProjectFromSlug(input)
    }

    abstract suspend fun requestProjectFromId(id: String): Project?
    abstract suspend fun requestProjectFromSlug(slug: String): Project?


    /**
     * Requests project files based on lists of Minecraft versions and loaders, and a file ID.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The file ID.
     * @return A mutable list of [ProjectFile] objects obtained by combining project files from the specified
     *         Minecraft versions and loaders. The list may be empty if no files are found for any combination.
     */
    suspend fun requestProjectFilesFromId(
        mcVersions: List<String>, loaders: List<String>, input: String
    ): MutableList<ProjectFile>
    {
        val result = mutableListOf<ProjectFile>()
        for (mcVersion in mcVersions)
        {
            for (loader in loaders) requestProjectFilesFromId(mcVersion, loader, input)?.let {
                result.addAll(it)
            }
        }
        return result
    }

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param input The file ID.
     * @return A mutable list of [ProjectFile] objects, or null if an error occurs or no files are found.
     */
    abstract suspend fun requestProjectFilesFromId(
        mcVersion: String, loader: String, input: String
    ): MutableList<ProjectFile>?


    private fun init() = platforms.add(this)

    init
    {
        init()
    }
}