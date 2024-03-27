package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.inheritPropertiesFrom

object Multiplatform : IProjectProvider
{
    /** List of registered platforms. */
    val platforms = listOf(
        CurseForge,
        Modrinth
    )

    /**
     * Requests a [project][Project] from all platforms.
     *
     * This function takes a string and attempts to retrieve a project from all platforms.
     * If the project is missing on one platform, it attempts to retrieve it from the other using platform-specific slugs.
     * The result is a combined project; project from the available platform; or null if no
     * project is found on either platform.
     *
     * @param input The project ID or slug.
     * @return A [project][Project] containing data retrieved from all platforms, or null if no data is found.
     */
    override suspend fun requestProject(input: String): Project?
    {
        var cf = CurseForge.requestProject(input)
        var mr = Modrinth.requestProject(input)

        // Retrieve project from another platform if it's missing.
        if (cf == null && mr != null)
        {
            cf = CurseForge.requestProjectFromSlug(mr.slug[Modrinth.serialName]!!)
        }
        else if (mr == null && cf != null)
        {
            mr = Modrinth.requestProjectFromSlug(cf.slug[CurseForge.serialName]!!)
        }

        // Combine projects or return just one of them.
        return cf?.let { c ->
            mr?.let { m ->
                c + m // Combine projects if project is available from both platforms.
            } ?: c // Return the CurseForge project if Modrinth project is missing.
        } ?: mr // Return the Modrinth project if CurseForge project is missing.
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
        mcVersions: List<String>, loaders: List<String>, input: String, fileId: String?, numberOfFiles: Int
    ): Project?
    {
        val project = requestProject(input) ?: return null

        if (fileId == null)
        {
            for (platform in platforms)
            {
                project.files.addAll(platform.requestFilesForProject(mcVersions, loaders, project, null, numberOfFiles))
            }
        }
        else
        {
            if (project.isOnPlatform(CurseForge))
            {
                val cfFile =
                    CurseForge.requestProjectFiles(mcVersions, loaders, project.id[CurseForge.serialName]!!, fileId)
                        .firstOrNull()

                val hash = cfFile?.hashes?.get("sha1")

                if (hash != null)
                {
                    val mrFile =
                        Modrinth.requestMultipleProjectFilesFromHashes(listOf(hash), "sha1")
                            .firstOrNull()

                    if (mrFile != null)
                    {
                        project.files.add(mrFile)
                    }

                    project.files.add(cfFile)
                }
            }

            if (project.isOnPlatform(Modrinth))
            {
                val mrFile =
                    Modrinth.requestProjectFiles(mcVersions, loaders, project.id[Modrinth.serialName]!!, fileId)
                        .firstOrNull()

                val bytes = mrFile?.url?.let { Modrinth.requestByteArray(it) }

                if (bytes != null)
                {
                    val cfFile =
                        CurseForge.requestMultipleProjectFilesFromBytes(mcVersions, listOf(bytes))
                            .firstOrNull()

                    if (cfFile != null)
                    {
                        project.files.add(cfFile)
                    }

                    project.files.add(mrFile)
                }
            }
        }

        return project
    }

    /**
     * Requests new data for provided [projects] from all platforms and updates them based on platform-specific slugs,
     * with optional [number of files][numberOfFiles] to take.
     * Projects are also filtered using their [update strategy][UpdateStrategy].
     */
    suspend fun updateMultipleProjectsWithFiles(
        mcVersions: List<String>,
        loaders: List<String>,
        projects: MutableSet<Project>,
        configFile: ConfigFile?,
        numberOfFiles: Int
    ): MutableSet<Project>
    {
        return platforms.fold(projects.map { it.copy(files = mutableSetOf()) }.toMutableSet()) { acc, platform ->

            val listOfIds = projects.mapNotNull { it.id[platform.serialName] }

            platform.requestMultipleProjectsWithFiles(mcVersions, loaders, listOfIds, numberOfFiles)
                .inheritPropertiesFrom(configFile)
                .forEach { newProject ->
                    acc.find { accProject ->
                        accProject.slug[platform.serialName] == newProject.slug[platform.serialName]
                    }?.let { accProject ->
                        acc -= accProject
                        acc += accProject + newProject
                    }
                }

            acc
        }.filter { newProject ->
            projects.none { oldProject ->
                oldProject == newProject
            } && newProject.updateStrategy == UpdateStrategy.LATEST
        }.toMutableSet()
    }
}