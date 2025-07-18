package teksturepako.pakku.api.platforms

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import kotlinx.datetime.Instant
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.PakkuApi
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.http.requestBody
import teksturepako.pakku.api.http.tryRequest
import teksturepako.pakku.api.models.gh.GhReleaseModel
import teksturepako.pakku.api.models.gh.GhRepoModel
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

object GitHub : Provider
{
    override val name = "GitHub"
    override val serialName = "github"
    override val shortName = "gh"
    override val siteUrl = "https://github.com"

    // -- ACCESS TOKEN --

    private val accessTokenHeader = PakkuApi.gitHubAccessToken
        ?.takeIf { it.isNotBlank() }
        ?.let { "Authorization" to "token $it" }

    // -- PROJECT --

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

    override suspend fun requestProject(input: String, projectType: ProjectType?): Result<Project, ActionError> = tryRequest {
        val responseString = requestBody("https://api.github.com/repos/$input", accessTokenHeader)
            .getOrElse { return Err(it) }

        val project = json.decodeFromString<GhRepoModel>(responseString)
            .toProject()
            .also { project -> projectType?.let { project.type = it } }

        return Ok(project)
    }

    // -- PROJECT FILES --

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
                hashes = asset.digest?.split(":")?.let { digest ->
                    val algo = digest.getOrNull(0) ?: return@let null
                    val hash = digest.getOrNull(1) ?: return@let null

                    mutableMapOf(algo to hash)
                },
                requiredDependencies = null,
                size = asset.size,
                datePublished = Instant.parse(publishedAt ?: createdAt)
            )
        }.asReversed()
    }

    override suspend fun requestProjectWithFiles(
        mcVersions: List<String>,
        loaders: List<String>,
        input: String,
        fileId: String?,
        numberOfFiles: Int,
        projectType: ProjectType?
    ): Result<Project, ActionError> = tryRequest {
        val project = requestProject(input, projectType).getOrElse { return Err(it) }

        val projectFiles = if (fileId == null) // Multiple files
        {
            val responseString = requestBody("https://api.github.com/repos/$input/releases", accessTokenHeader)
                .getOrElse { return Err(it) }

            json.decodeFromString<List<GhReleaseModel>>(responseString)
                .flatMap { it.toProjectFiles(project.id[this.serialName]!!).take(1) }
                .take(numberOfFiles)
        }
        else // One file
        {
            val responseString = requestBody("https://api.github.com/repos/$input/releases/tags/$fileId", accessTokenHeader)
                .getOrElse { return Err(it) }

            json.decodeFromString<GhReleaseModel>(responseString)
                .toProjectFiles(project.id[this.serialName]!!)
                .take(numberOfFiles)
        }

        return Ok(project.apply { files.addAll(projectFiles) })
    }

}