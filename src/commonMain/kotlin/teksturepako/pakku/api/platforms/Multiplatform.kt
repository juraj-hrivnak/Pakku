package teksturepako.pakku.api.platforms

import com.github.michaelbull.result.*
import io.ktor.http.*
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.http.RequestError
import teksturepako.pakku.api.http.requestByteArray
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType

object Multiplatform : Provider
{
    override val name = "Multiplatform"
    override val serialName = "multiplatform"
    override val shortName = "mp"
    override val siteUrl = null

    /** List of registered platforms. */
    val platforms = listOf(
        CurseForge,
        Modrinth
    )

    fun getPlatform(serialName: String) = this.platforms.find {
        it.serialName == serialName
    }

    /**
     * Requests a [project][Project] from all platforms.
     *
     * This function takes a string and attempts to retrieve a project from all platforms.
     * If the project is missing on one platform, it attempts to retrieve it from the other using platform-specific slugs.
     * The result is a combined project; project from the available platform; or null if no
     * project is found on either platform.
     *
     * @param input The project ID or slug.
     * @param projectType The type of project.
     * @return A [project][Project] containing data retrieved from all platforms, or null if no data is found.
     */
    override suspend fun requestProject(input: String, projectType: ProjectType?): Result<Project, ActionError>
    {
        var cf = CurseForge.requestProject(input, projectType).fold(
            success = { it },
            failure = { error ->
                if (error is ProjNotFound || (error is RequestError && error.response.status == HttpStatusCode.NotFound))
                {
                    null
                }
                else return Err(error)
            }
        )
        var mr = Modrinth.requestProject(input, projectType).fold(
            success = { it },
            failure = { error ->
                if (error is ProjNotFound || (error is RequestError && error.response.status == HttpStatusCode.NotFound))
                {
                    null
                }
                else return Err(error)
            }
        )

        // Retrieve project from another platform if it's missing.
        if (cf == null && mr != null)
        {
            cf = CurseForge.requestProjectFromSlug(mr.slug[Modrinth.serialName]!!).fold(
                success = { it },
                failure = { error ->
                    if (error is ProjNotFound || (error is RequestError && error.response.status == HttpStatusCode.NotFound))
                    {
                        null
                    }
                    else return Err(error)
                }
            )
        }
        else if (mr == null && cf != null)
        {
            mr = Modrinth.requestProject(cf.slug[CurseForge.serialName]!!).fold(
                success = { it },
                failure = { error ->
                    if (error is ProjNotFound || (error is RequestError && error.response.status == HttpStatusCode.NotFound))
                    {
                        null
                    }
                    else return Err(error)
                }
            )
        }

        // Combine projects or return just one of them.
        val combinedProject = cf?.let { c ->
            projectType?.let { c.type = it }
            mr?.let { m ->
                projectType?.let { m.type = it }
                (c + m).get() // Combine projects if project is available from both platforms.
            } ?: c // Return the CurseForge project if Modrinth project is missing.
        } ?: mr // Return the Modrinth project if CurseForge project is missing.

        if (combinedProject == null) return Err(ProjNotFound())

        return Ok(combinedProject)
    }

     /**
     * [Requests a project][requestProject] with files from all platforms, and returns a [project][Project],
     * with optional [number of files][numberOfFiles] to take.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The project ID or slug.
     * @param numberOfFiles The number of requested files for each platform. Defaults to 1.
     * @return A [project][Project] with files from all platforms or null if the initial project request is unsuccessful.
     */
    override suspend fun requestProjectWithFiles(
         mcVersions: List<String>,
         loaders: List<String>,
         input: String,
         fileId: String?,
         numberOfFiles: Int ,
         projectType: ProjectType?
    ): Result<Project, ActionError>
    {
        val project = requestProject(input, projectType).getOrElse { return Err(it) }

        if (fileId == null)
        {
            for (platform in platforms)
            {
                val files = platform.requestFilesForProject(mcVersions, loaders, project, null, numberOfFiles, projectType)
                    .get() ?: mutableSetOf()

                project.files.addAll(files)
            }
        }
        else
        {
            if (project.isOnPlatform(CurseForge))
            {
                val cfFile = CurseForge.requestProjectFiles(mcVersions, loaders, project.id[CurseForge.serialName]!!, fileId, projectType)
                    .get()?.firstOrNull()


                val hash = cfFile?.hashes?.get("sha1")

                if (hash != null)
                {
                    val mrFile = Modrinth.requestMultipleProjectFilesFromHashes(listOf(hash), "sha1")
                        .get()?.firstOrNull()

                    if (mrFile != null)
                    {
                        project.files.add(mrFile)
                    }

                    project.files.add(cfFile)
                }
            }

            if (project.isOnPlatform(Modrinth))
            {
                val mrFile = Modrinth.requestProjectFiles(mcVersions, loaders, project.id[Modrinth.serialName]!!, fileId, projectType)
                    .get()?.firstOrNull()

                val bytes = mrFile?.url?.let { requestByteArray(it).get() }

                if (bytes != null)
                {
                    val cfFile = CurseForge.requestMultipleProjectFilesFromBytes(mcVersions, listOf(bytes))
                        .get()?.firstOrNull()

                    if (cfFile != null)
                    {
                        project.files.add(cfFile)
                    }

                    project.files.add(mrFile)
                }
            }
        }

        return Ok(project)
    }
}