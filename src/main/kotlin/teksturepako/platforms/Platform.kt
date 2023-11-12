package teksturepako.platforms

import teksturepako.http.Http
import teksturepako.platforms.projects.Project

/**
 * Platform is a site containing projects and can request them (mods).
 */
abstract class Platform : Http()
{
    /**
     * The api URL address of this platform.
     */
    abstract val apiUrl: String

    /**
     * Version of the API.
     */
    abstract val apiVersion: Int

    suspend fun requestProjectString(input: String): String?
    {
        return this.requestBody("$apiUrl/v$apiVersion/$input")
    }

    suspend fun requestProject(input: String): Project?
    {
        return requestProjectFromId(input) ?: requestProjectFromSlug(input)
    }

    abstract suspend fun requestProjectFromSlug(slug: String): Project?
    abstract suspend fun requestProjectFromId(id: String): Project?

}
