package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

/**
 * Platform is a site containing projects.
 * @param name Platform name.
 * @param serialName Snake case version of the name.
 * @param apiUrl The API URL address of this platform.
 * @param apiVersion Version of the API.
 */
abstract class Platform(
    val name: String,
    val serialName: String,
    val shortName: String,
    val apiUrl: String,
    val apiVersion: Int,
    val siteUrl: String,
) : Http(), IProjectProvider
{
    override fun toString(): String = this.name

    abstract fun getUrlForProjectType(projectType: ProjectType): String

    suspend fun requestProjectBody(input: String): String? =
        this.requestBody("$apiUrl/v$apiVersion/$input")

    suspend inline fun <reified T> requestProjectBody(input: String, bodyContent: T): String? =
        this.requestBody("$apiUrl/v$apiVersion/$input", bodyContent)

    /** Requests a [project][Project] based on either its ID or slug. */
    abstract override suspend fun requestProject(input: String): Project?

    /** Requests a [project][Project] using its [ID][id]. */
    abstract suspend fun requestProjectFromId(id: String): Project?

    /** Requests a [project][Project] using its [slug]. */
    abstract suspend fun requestProjectFromSlug(slug: String): Project?

    abstract suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>

    // -- FILES --

    val validLoaders = listOf("minecraft", "iris", "optifine", "datapack")

    /**
     * Requests [project files][ProjectFile] based on [minecraft versions][mcVersions], [loaders], [projectId] or
     * [projectId] & [fileId].
     */
    abstract suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String? = null
    ): MutableSet<ProjectFile>

    abstract suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>
    ): MutableSet<ProjectFile>


    /**
     * [Requests project files][requestProjectFiles] for provided [project][Project], with optional
     * [number of files][numberOfFiles] to take.
     */
    suspend fun requestFilesForProject(
        mcVersions: List<String>, loaders: List<String>, project: Project, fileId: String? = null, numberOfFiles: Int = 1
    ): MutableSet<ProjectFile>
    {
        return project.id[this.serialName]?.let { projectId ->
            this.requestProjectFiles(mcVersions, loaders, projectId, fileId).take(numberOfFiles).toMutableSet()
        } ?: mutableSetOf()
    }

    /**
     * [Requests a project][requestProject] with [files][requestFilesForProject], and returns a [project][Project],
     * with optional [number of files][numberOfFiles] to take.
     */
    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, fileId: String?, numberOfFiles: Int
    ): Project?
    {
        return requestProject(input)?.apply {
            files.addAll(requestFilesForProject(mcVersions, loaders, this, fileId, numberOfFiles))
        }
    }

    abstract suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>, numberOfFiles: Int
    ): MutableSet<Project>
}