package teksturepako.pakku.api.platforms

import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.models.gh.GhReleaseModel
import teksturepako.pakku.api.models.gh.GhRepoModel
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

object GitHub : Http(), IProjectProvider
{
    override val name = "GitHub"
    override val serialName = "github"
    override val shortName = "gh"

    private fun GhRepoModel.toProject(): Project
    {
        return Project(
            name = mutableMapOf(serialName to name),
            slug = mutableMapOf(serialName to fullName),
            type = ProjectType.MOD, // Defaults to MOD; This can not be detected.
            id = mutableMapOf(serialName to id.toString()),
            redistributable = license != null && license.spdxId != "ARR",
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProject(input: String): Project?
    {
        return json.decodeFromString<GhRepoModel>(
            this.requestBody("https://api.github.com/repos/$input")
                ?: return null
        ).toProject()
    }

    private fun GhReleaseModel.toProjectFiles(parentId: String): List<ProjectFile>
    {
        return this.assets.map { asset ->
            ProjectFile(
                type = serialName,
                fileName = asset.name,
                mcVersions = mutableListOf(),
                loaders = mutableListOf(),
                releaseType = when
                {
                    "alpha" in tagName -> "alpha"
                    "beta" in tagName  -> "beta"
                    else               -> "release"
                },
                url = UrlEncoderUtil.decode(asset.browserDownloadUrl), // Decode URL
                id = asset.id.toString(),
                parentId = parentId,
                hashes = null,
                requiredDependencies = null,
                size = asset.size,
            )
        }.asReversed()
    }

    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, fileId: String?, numberOfFiles: Int
    ): Project?
    {
        val project = requestProject(input) ?: return null

        val projectFiles = json.decodeFromString<List<GhReleaseModel>>(
        this.requestBody("https://api.github.com/repos/$input/releases") ?: return null
        )
            .flatMap { it.toProjectFiles(project.id[this.serialName]!!).take(numberOfFiles) }
            .take(numberOfFiles)

        return project.apply { files.addAll(projectFiles) }
    }

}