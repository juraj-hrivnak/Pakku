package teksturepako.pakku.api.platforms

import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.models.gh.GhReleaseModel
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

object GitHub : Http(), IProjectProvider
{
    override val name = "GitHub"
    override val serialName = "github"
    override val shortName = "gh"

    fun GhReleaseModel.toProject(): Project
    {
        return Project(
            name = mutableMapOf(), //TODO
            slug = mutableMapOf(), //TODO
            type = ProjectType.MOD, //TODO
            id = mutableMapOf(serialName to id.toString()),
            redistributable = true, //TODO
            files = assets.map { asset: GhReleaseModel.Asset ->
                ProjectFile(
                    type = this@GitHub.serialName,
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
                    parentId = id.toString(),
                    hashes = null,
                    requiredDependencies = null,
                    size = asset.size,
                )
            }.asReversed().toMutableSet(),
        )
    }

    override suspend fun requestProject(input: String): Project?
    {
        return json.decodeFromString<List<GhReleaseModel>>(
            this.requestBody("https://api.github.com/repos/$input/releases")
                ?: return null
        )
            .map { it.toProject() }
            .firstOrNull()
    }

    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>, loaders: List<String>, input: String, fileId: String?, numberOfFiles: Int
    ): Project?
    {
        TODO("Not yet implemented")
    }

}