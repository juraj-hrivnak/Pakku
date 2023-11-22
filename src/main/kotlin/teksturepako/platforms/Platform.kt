package teksturepako.platforms

import teksturepako.http.Http
import teksturepako.platforms.Multiplatform.platforms
import teksturepako.projects.Project
import teksturepako.projects.ProjectFile

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

    abstract suspend fun requestProjectFilesFromId(
        mcVersion: String, loader: String, input: String
    ): MutableList<ProjectFile>?


    private fun init() = platforms.add(this)

    init
    {
        init()
    }
}