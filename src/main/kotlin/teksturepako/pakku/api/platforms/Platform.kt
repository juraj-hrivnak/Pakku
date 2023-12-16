package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

/**
 * Platform is a site containing projects.
 */
abstract class Platform : Http(), IProjectProvider
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

    /**
     * Requests a project based on either its ID or slug.
     *
     * @param input The project ID or slug.
     * @return A [Project] instance if found, or null if the project with the specified ID or slug is not found.
     */
    override suspend fun requestProject(input: String): Project?
    {
        return requestProjectFromId(input) ?: requestProjectFromSlug(input)
    }

    /**
     * Requests a project using its unique identifier (ID).
     *
     * @param id The unique identifier (ID) of the project to request.
     * @return A [Project] instance if the project with the specified ID is found, otherwise null.
     */
    abstract suspend fun requestProjectFromId(id: String): Project?

    /**
     * Requests a project using its slug.
     *
     * @param slug The slug of the project to request.
     * @return A [Project] instance if the project with the specified slug is found, otherwise null.
     */
    abstract suspend fun requestProjectFromSlug(slug: String): Project?


    /**
     * Requests project files based on lists of Minecraft versions and loaders, and a file ID.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param fileId The file ID.
     * @return A mutable list of [ProjectFile] objects obtained by combining project files from the specified
     *         Minecraft versions and loaders. The list may be empty if no files are found for any combination.
     */
    suspend fun requestProjectFilesFromFileId(
        mcVersions: List<String>, loaders: List<String>, fileId: String
    ): MutableSet<ProjectFile>
    {
        val result = mutableSetOf<ProjectFile>()
        for (mcVersion in mcVersions)
        {
            for (loader in loaders) requestProjectFilesFromFileId(mcVersion, loader, fileId)?.let {
                for (projectFile in it)
                {
                    if (projectFile !in result) result.add(projectFile)
                }
            }
        }
        return result
    }

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param fileId The file ID.
     * @return A mutable list of [ProjectFile] objects, or null if an error occurs or no files are found.
     */
    abstract suspend fun requestProjectFilesFromFileId(
        mcVersion: String, loader: String, fileId: String
    ): MutableSet<ProjectFile>?

    /**
     * Requests project files based on specified Minecraft versions, loaders, a project, and the desired number of
     * files.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param project The [Project] for which to request files.
     * @param numberOfFiles The number of requested files. Defaults to 1.
     * @return A mutable set of [ProjectFile] instances associated with the specified project.
     */
    abstract suspend fun requestFilesForProject(
        mcVersions: List<String>, loaders: List<String>, project: Project, numberOfFiles: Int = 1
    ): MutableSet<ProjectFile>

    /**
     * Requests a project along with project files based on specified Minecraft versions, loaders, its
     * ID or slug, and the desired number of files.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The project ID or slug.
     * @param numberOfFiles The number of requested files. Defaults to 1.
     * @return A [Project] instance if found, or null if the project with the specified ID or slug is not found.
     *         If the project is found, the project files are added to the project's file set.
     */
    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, numberOfFiles: Int
    ): Project?
    {
        val project = requestProject(input) ?: return null

        project.files.addAll(requestFilesForProject(mcVersions, loaders, project, numberOfFiles))

        return project
    }

//    abstract suspend fun requestDependenciesForProject(project: Project): List<Project>
}