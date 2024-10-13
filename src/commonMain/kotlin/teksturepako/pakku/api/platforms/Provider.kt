package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.projects.Project

interface Provider
{
    companion object
    {
        val providers = listOf(
            CurseForge,
            GitHub,
            Modrinth,
            Multiplatform
        )

        fun getProvider(serialName: String) = this.providers.find {
            it.serialName == serialName
        }
    }

    val name: String

    val serialName: String

    val shortName: String

    val siteUrl: String?

    /** Requests a [project][Project] based on provided [input]. */
    suspend fun requestProject(input: String): Project?

    /**
     * Requests project with files for specified combinations of Minecraft versions and mod loaders.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loaders.
     * @param input The input for the project files request.
     * @param numberOfFiles The number of files to take. Defaults to 1.
     * @return A [Project] with requested project files, or null if no data is found.
     */
    suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, fileId: String? = null, numberOfFiles: Int = 1
    ): Project?
}