package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

/** Platform is a site containing projects. */
abstract class Platform : Http(), IProjectProvider
{
    /** Platform name. */
    abstract val name: String

    /** Snake case version of the name. */
    abstract val serialName: String

    /** The API URL address of this platform. */
    abstract val apiUrl: String

    /** Version of the API. */
    abstract val apiVersion: Int

    suspend fun requestProjectBody(input: String): String? = this.requestBody("$apiUrl/v$apiVersion/$input")

    /** Requests a [project][Project] based on either its ID or slug. */
    abstract override suspend fun requestProject(input: String): Project?

    /** Requests a [project][Project] using its [ID][id]. */
    abstract suspend fun requestProjectFromId(id: String): Project?

    /** Requests a [project][Project] using its [slug]. */
    abstract suspend fun requestProjectFromSlug(slug: String): Project?

    /**
     * Requests [project files][ProjectFile] based on [minecraft versions][mcVersions], [loaders], [projectId] or
     * [projectId] & [fileId].
     */
    abstract suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String? = null
    ): MutableSet<ProjectFile>

    /**
     * [Requests project files][requestProjectFiles] for provided [project][Project], with optional
     * [number of files][numberOfFiles] to take.
     */
    suspend fun requestFilesForProject(
        mcVersions: List<String>, loaders: List<String>, project: Project, numberOfFiles: Int = 1
    ): MutableSet<ProjectFile> = project.id[this.serialName]?.let { projectId ->
        this.requestProjectFiles(mcVersions, loaders, projectId).take(numberOfFiles).toMutableSet()
    } ?: mutableSetOf()

    /**
     * [Requests a project][requestProject] and [files][requestFilesForProject], and returns a [project][Project],
     * with optional [number of files][numberOfFiles] to take.
     */
    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, numberOfFiles: Int
    ): Project? = requestProject(input)?.apply {
        files.addAll(requestFilesForProject(mcVersions, loaders, this, numberOfFiles))
    }

    /**
     * [Requests a project][requestProjectFromId] with [files][requestProjectFiles] using its [projectId] & [fileId],
     * with optional [number of files][numberOfFiles] to take.
     */
    suspend fun requestProjectWithFilesFromIds(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String, numberOfFiles: Int = 1
    ): Project? = requestProjectFromId(projectId)?.apply {
        files.addAll(requestProjectFiles(mcVersions, loaders, projectId, fileId).take(numberOfFiles))
    }
}