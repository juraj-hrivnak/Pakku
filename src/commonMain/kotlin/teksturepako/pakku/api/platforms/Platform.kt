package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.http.Http
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
    override val name: String,
    override val serialName: String,
    override val shortName: String,
    val apiUrl: String,
    val apiVersion: Int,
    override val siteUrl: String,
) : Http(), Provider
{
    override fun toString(): String = this.name

    abstract fun getUrlForProjectType(projectType: ProjectType): String

    open fun getCommonRequestUrl(
        apiUrl: String = this.apiUrl,
        apiVersion: Int = this.apiVersion
    ) = "$apiUrl/v$apiVersion"

    suspend fun requestProjectBody(input: String): String? =
        this.requestBody("${this.getCommonRequestUrl()}/$input")

    suspend fun requestProjectBody(input: String, bodyContent: () -> String): String? =
        this.requestBody("${this.getCommonRequestUrl()}/$input", bodyContent)

    abstract suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>

    // -- FILES --

    /**
     * Requests [project files][ProjectFile] based on [minecraft versions][mcVersions], [loaders], [projectId] or
     * [projectId] & [fileId].
     */
    abstract suspend fun requestProjectFiles(
        mcVersions: List<String>,
        loaders: List<String>,
        projectId: String,
        fileId: String? = null,
        projectType: ProjectType? = null
    ): MutableSet<ProjectFile>

    abstract suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, ids: List<String>
    ): MutableSet<ProjectFile>


    /**
     * [Requests project files][requestProjectFiles] for provided [project][Project], with optional
     * [number of files][numberOfFiles] to take.
     */
    open suspend fun requestFilesForProject(
        mcVersions: List<String>,
        loaders: List<String>,
        project: Project,
        fileId: String? = null,
        numberOfFiles: Int = 1,
        projectType: ProjectType? = null
    ): MutableSet<ProjectFile>
    {
        return project.id[this.serialName]?.let { projectId ->
            this.requestProjectFiles(mcVersions, loaders, projectId, fileId, projectType).take(numberOfFiles).toMutableSet()
        } ?: mutableSetOf()
    }

    /**
     * [Requests a project][requestProject] with [files][requestFilesForProject], and returns a [project][Project],
     * with optional [number of files][numberOfFiles] to take.
     */
    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>,
        loaders: List<String>,
        input: String,
        fileId: String?,
        numberOfFiles: Int,
        projectType: ProjectType?
    ): Project?
    {
        return requestProject(input, projectType)?.apply {
            files.addAll(requestFilesForProject(mcVersions, loaders, this, fileId, numberOfFiles, projectType))
        }
    }

    abstract suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, numberOfFiles: Int
    ): MutableSet<Project>

    companion object
    {
        val validLoaders = listOf("minecraft", "iris", "optifine", "datapack")
    }
}