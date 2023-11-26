package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.projects.CfFile
import teksturepako.pakku.api.projects.Project

object Multiplatform
{
    /** List of registered platforms. */
    val platforms = ArrayList<Platform>()

    /**
     * Requests a project from all platforms based on the provided input.
     *
     * This function takes an input string and attempts to retrieve project details from all platforms.
     * If the project is missing on one platform, it attempts to retrieve it from the other using platform-specific slugs.
     * The result is a combined [Project] object if details are available from all platforms, otherwise, it returns the
     * [Project] object from the available platform or null if no details are found on either platform.
     *
     * @param input The input string used to identify the project.
     * @return A [Project] object containing data retrieved from all platforms, or null if no data is found.
     */
    suspend fun requestProject(input: String): Project?
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
     * Requests project files for specified combinations of Minecraft versions and mod loaders from all platforms.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The common input for the project files request.
     * @param numberOfFiles The number of requested files for each platform. Defaults to 1.
     * @return A [Project] object with requested project files from all platforms.
     *         Returns null if the initial project request is unsuccessful.
     */
    suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, numberOfFiles: Int = 1
    ): Project?
    {
        val project = requestProject(input) ?: return null

        // CurseForge
        project.id[CurseForge.serialName].finalize().let { projectId ->
            CurseForge.requestProjectFilesFromId(mcVersions, loaders, projectId)
                .take(numberOfFiles)
                .filterIsInstance<CfFile>()
                .forEach { file ->
                    // Request URL if is null and add to project files.
                    if (file.url != "null") project.files.add(file) else
                    {
                        val url = CurseForge.fetchAlternativeDownloadUrl(file.id, file.fileName)
                        project.files.add(file.apply {
                            // Replace empty characters in the URL.
                            this.url = url.replace(" ", "%20")
                        })
                    }
                }
        }

        // Modrinth
        project.id[Modrinth.serialName].finalize().let { projectId ->
            Modrinth.requestProjectFilesFromId(mcVersions, loaders, projectId)
                .take(numberOfFiles)
                .also { project.files.addAll(it) }
        }

        return project
    }
}