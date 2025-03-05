package teksturepako.pakku.api.platforms

import com.github.michaelbull.result.*
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.http.requestBody
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
) : Provider
{
    override fun toString(): String = this.name

    abstract fun getUrlForProjectType(projectType: ProjectType): String

    open fun getCommonRequestUrl(
        apiUrl: String = this.apiUrl,
        apiVersion: Int = this.apiVersion
    ) = "$apiUrl/v$apiVersion"

    suspend fun requestProjectBody(input: String): Result<String, ActionError> =
        requestBody("${this.getCommonRequestUrl()}/$input")

    suspend fun requestProjectBody(input: String, bodyContent: () -> String): Result<String, ActionError> =
        requestBody("${this.getCommonRequestUrl()}/$input", bodyContent)

    abstract suspend fun requestMultipleProjects(ids: List<String>): Result<MutableSet<Project>, ActionError>

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
    ): Result<MutableSet<ProjectFile>, ActionError>

    abstract suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, ids: List<String>
    ): Result<MutableSet<ProjectFile>, ActionError>

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
    ): Result<MutableSet<ProjectFile>, ActionError>
    {
        return Ok(project.id[this.serialName]?.let { projectId ->
            this.requestProjectFiles(mcVersions, loaders, projectId, fileId, projectType)
                .getOrElse { return Err(it) }
                .take(numberOfFiles).toMutableSet()
        } ?: mutableSetOf())
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
    ): Result<Project, ActionError>
    {
        return Ok(requestProject(input, projectType)
            .getOrElse { return Err(it) }
            .apply {
                val files = requestFilesForProject(mcVersions, loaders, this, fileId, numberOfFiles, projectType).get()

                if (files != null)
                {
                    this.files.addAll(files)
                }
            }
        )
    }

    abstract suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, numberOfFiles: Int
    ): Result<MutableSet<Project>, ActionError>

    companion object
    {
        val validLoaders = listOf("minecraft", "iris", "optifine", "datapack")
    }
}