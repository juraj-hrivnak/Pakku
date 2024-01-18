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
            cf = CurseForge.requestProjectFromSlug(mr.slug[Modrinth.serialName]!!)
        }
        else if (mr == null && cf != null)
        {
            mr = Modrinth.requestProjectFromSlug(cf.slug[CurseForge.serialName]!!)
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

    /** @return List of updated projects. */
    suspend fun updateMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projects: MutableSet<Project>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        return platforms.fold(projects.map { it.copy(files = mutableSetOf()) }.toMutableSet()) { acc, platform ->

            val listOfIds = projects.mapNotNull { it.id[platform.serialName] }

            platform.requestMultipleProjectsWithFiles(mcVersions, loaders, listOfIds, numberOfFiles).forEach { project ->
                acc.find { it.slug[platform.serialName] == project.slug[platform.serialName] }?.let {
                    acc -= it
                    acc += it + project
                }
            }

            acc
        }.filter { projects.none { project -> project == it } }.toMutableSet()
    }
}