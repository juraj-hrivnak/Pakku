package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project

object Multiplatform : IProjectProvider
{
    /** List of registered platforms. */
    val platforms = listOf(
        CurseForge,
        Modrinth
    )

    /**
     * Requests a project from all platforms based on the provided input.
     *
     * This function takes an input string and attempts to retrieve project details from all platforms.
     * If the project is missing on one platform, it attempts to retrieve it from the other using platform-specific slugs.
     * The result is a combined [Project] object if details are available from all platforms, otherwise, it returns the
     * [Project] object from the available platform or null if no details are found on either platform.
     *
     * @param input The project ID or slug.
     * @return A [Project] object containing data retrieved from all platforms, or null if no data is found.
     */
    override suspend fun requestProject(input: String): Project?
    {
        var cf = CurseForge.requestProject(input)
        var mr = Modrinth.requestProject(input)

        // Retrieve project from another platform if it's missing.
        if (cf == null && mr != null)
        {
            cf = CurseForge.requestProjectFromSlug(mr.slug[Modrinth.serialName]!!.replace("\"", ""))
        } else if (mr == null && cf != null)
        {
            mr = Modrinth.requestProjectFromSlug(cf.slug[CurseForge.serialName]!!.replace("\"", ""))
        }

        // Combine projects or return one just of them
        return cf?.let { c ->
            mr?.let { m ->
                c + m // Combine projects if project is available from both platforms.
            } ?: c // Return the CurseForge project if Modrinth project is missing.
        } ?: mr // Return the Modrinth project if CurseForge project is missing.
    }

    /**
     * Requests project with files for specified combinations of Minecraft versions and mod loaders for all platforms.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The project ID or slug.
     * @param numberOfFiles The number of requested files for each platform. Defaults to 1.
     * @return A [Project] object with requested project files from all platforms.
     *         Returns null if the initial project request is unsuccessful.
     */
    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, numberOfFiles: Int
    ): Project?
    {
        val project = requestProject(input) ?: return null

        for (platform in platforms)
        {
            project.files.addAll(platform.requestFilesForProject(mcVersions, loaders, project, numberOfFiles))
        }

        return project
    }

    suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, ids: Map<String, List<String>>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        val result: MutableSet<Project> = mutableSetOf()

        val projectsCf = ids[CurseForge.serialName]?.let {
            CurseForge.requestMultipleProjectsWithFiles(mcVersions, loaders, it, numberOfFiles)
        }
        val projectsMr = ids[Modrinth.serialName]?.let {
            Modrinth.requestMultipleProjectsWithFiles(mcVersions, loaders, it, numberOfFiles)
        }

        when
        {
            projectsCf != null && projectsMr != null ->
            {
                for (projectCf in projectsCf)
                {
                    for (projectMr in projectsMr)
                    {
                        if (projectCf isAlmostTheSameAs projectMr) result += projectCf + projectMr
                    }
                }
            }
            projectsCf != null -> result.addAll(projectsCf)
            projectsMr != null -> result.addAll(projectsMr)
        }

        return result
    }
}