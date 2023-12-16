package teksturepako.pakku.api.projects

interface IProjectProvider
{
    /**
     * Requests a project based on the provided input.
     *
     * @param input The input string used to identify the project.
     * @return A [Project] object, or null if no data is found.
     */
    suspend fun requestProject(input: String): Project?

    /**
     * Requests project with files for specified combinations of Minecraft versions and mod loaders.
     *
     * @param mcVersions The list of Minecraft versions.
     * @param loaders The list of mod loader types.
     * @param input The common input for the project files request.
     * @param numberOfFiles The number of requested files for each platform. Defaults to 1.
     * @return A [Project] object with requested project files, or null if no data is found.
     */
    suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, numberOfFiles: Int = 1
    ): Project?
}