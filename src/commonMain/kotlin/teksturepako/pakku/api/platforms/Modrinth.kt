package teksturepako.pakku.api.platforms

import com.github.michaelbull.result.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import net.thauvin.erik.urlencoder.UrlEncoderUtil.encode
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.mr.GetVersionsFromHashesRequest
import teksturepako.pakku.api.models.mr.MrProjectModel
import teksturepako.pakku.api.models.mr.MrVersionModel
import teksturepako.pakku.api.projects.*
import teksturepako.pakku.debugIfEmpty

object Modrinth : Platform(
    name = "Modrinth",
    serialName = "modrinth",
    shortName = "mr",
    apiUrl = "https://api.modrinth.com",
    apiVersion = 2,
    siteUrl = "https://modrinth.com"
)
{
    // -- URLS --

    override fun getUrlForProjectType(projectType: ProjectType): String = when (projectType)
    {
        ProjectType.MOD             -> "${this.siteUrl}/mod"
        ProjectType.RESOURCE_PACK   -> "${this.siteUrl}/resourcepack"
        ProjectType.DATA_PACK       -> "${this.siteUrl}/datapack"
        ProjectType.WORLD           -> this.siteUrl // Does not exist yet
        ProjectType.SHADER          -> "${this.siteUrl}/shader"
    }

    // -- PROJECT --

    override suspend fun requestProject(input: String, projectType: ProjectType?): Result<Project, ActionError>
    {
        val responseString = this.requestProjectBody("project/$input").getOrElse { return Err(it) }

        val project = json.decodeFromString<MrProjectModel>(responseString).toProject()
            .getOrElse { return Err(it) }
            .apply { projectType?.let { type = it } }

        return Ok(project)
    }

    private fun MrProjectModel.toProject(): Result<Project, ActionError>
    {
        return Ok(Project(
            name = mutableMapOf(serialName to title),
            slug = mutableMapOf(serialName to slug),
            type = when (projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "datapack"     -> ProjectType.DATA_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return Err(ProjectTypeNotSupported(slug, projectType))
            },
            side = when
            {
                serverSide == "required" && clientSide == "required" -> ProjectSide.BOTH
                serverSide != "required" && clientSide == "required" -> ProjectSide.CLIENT
                serverSide == "required" && clientSide != "required" -> ProjectSide.SERVER
                else -> ProjectSide.BOTH
            },
            id = mutableMapOf(serialName to id),
            redistributable = license.id != "ARR",
            files = mutableSetOf(),
        ))
    }

    override suspend fun requestMultipleProjects(ids: List<String>): Result<MutableSet<Project>, ActionError>
    {
        val url = encode("projects?ids=${ids.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

        val responseString = this.requestProjectBody(url).getOrElse { return Err(it) }

        val projects = json.decodeFromString<List<MrProjectModel>>(responseString)
            .mapNotNull { it.toProject().get() }.toMutableSet()

        return Ok(projects)
    }

    // -- FILES --

    private fun List<MrVersionModel>.filterFileModels(
        mcVersions: List<String>, loaders: List<String>
    ): List<MrVersionModel> = this
        .filter { version ->
            version.gameVersions.any { it in mcVersions } && version.loaders
                .takeIf { it.isNotEmpty() }
                ?.map { it.lowercase() }?.any {
                    it in loaders || it in validLoaders // Check default valid loaders
                } ?: true // If no loaders found, accept model
        }

    internal fun compareByLoaders(loaders: List<String>): (MrVersionModel) -> Comparable<*> = if (loaders.size <= 1)
    {
        { 0 }
    }
    else { version: MrVersionModel ->
        loaders.indexOfFirst { it in version.loaders }.let { if (it == -1) loaders.size else it }
    }

    private fun MrVersionModel.toProjectFiles(): List<ProjectFile>
    {
        return this.files.sortedBy { it.primary }.map { versionFile ->
            ProjectFile(
                type = this@Modrinth.serialName,
                fileName = versionFile.filename,
                mcVersions = this.gameVersions.toMutableList(),
                loaders = this.loaders.toMutableList(),
                releaseType = this.versionType.run {
                    if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                },
                url = UrlEncoderUtil.decode(versionFile.url), // Decode URL
                id = this.id,
                parentId = this.projectId,
                hashes = versionFile.hashes.let {
                    mutableMapOf(
                        "sha512" to it.sha512,
                        "sha1" to it.sha1
                    )
                },
                requiredDependencies = this.dependencies
                    .filter { "required" in it.dependencyType }
                    .mapNotNull { it.projectId }.toMutableSet(),
                size = versionFile.size,
                datePublished = Instant.parse(this.datePublished)
            )
        }.asReversed() // Reverse to make non source files first
    }

    private const val DATAPACK_LOADER = "datapack"

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?, projectType: ProjectType?
    ): Result<MutableSet<ProjectFile>, ActionError>
    {
        val actualLoaders = when (projectType)
        {
            ProjectType.DATA_PACK -> listOf(DATAPACK_LOADER)
            else                  -> loaders
        }

        return if (fileId == null) // Multiple files
        {
            val responseString = this.requestProjectBody("project/$projectId/version")
                .getOrElse { return Err(it) }

            val files = json.decodeFromString<List<MrVersionModel>>(responseString)
                .filterFileModels(mcVersions, actualLoaders)
                .sortedWith(compareBy(compareByLoaders(actualLoaders)))
                .flatMap { version -> version.toProjectFiles() }
                .debugIfEmpty {
                    println("${this::class.simpleName}#requestProjectFiles: file is null")
                }
                .toMutableSet()

            Ok(files)
        }
        else // One file
        {
            val responseString = this.requestProjectBody("version/$fileId")
                .getOrElse { return Err(it) }

            val files = json.decodeFromString<MrVersionModel>(responseString)
                .toProjectFiles()
                .toMutableSet()

            Ok(files)
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, ids: List<String>
    ): Result<MutableSet<ProjectFile>, ActionError> = coroutineScope {
        val loadersWithType = when
        {
            projectIdsToTypes.values.any { it == ProjectType.DATA_PACK } -> listOf(DATAPACK_LOADER)
            else                                                         -> listOf()
        } + loaders

        // Chunk requests if there are too many ids; Also do this in parallel
        val files = ids.chunked(1_000).map { list ->
            async {
                val url = encode("versions?ids=${list.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

                val responseString = this@Modrinth.requestProjectBody(url).getOrElse { return@async Err(it) }

                val files = json.decodeFromString<List<MrVersionModel>>(responseString)

                Ok(files)
            }
        }
            .awaitAll()
            .map { result ->
                result.getOrElse { return@coroutineScope Err(it) }
            }
            .flatten()
            .filterFileModels(mcVersions, loadersWithType)
            .sortedWith(
                compareByDescending<MrVersionModel> { Instant.parse(it.datePublished) }
                    .thenBy { file ->
                        compareByLoaders(projectIdsToTypes[file.projectId]?.let {
                            when (it)
                            {
                                ProjectType.DATA_PACK -> listOf(DATAPACK_LOADER)
                                else                  -> null
                            }
                        } ?: loaders)(file)
                    }
            )
            .flatMap { version -> version.toProjectFiles() }
            .toMutableSet()

        return@coroutineScope Ok(files)
    }

    override suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, numberOfFiles: Int
    ): Result<MutableSet<Project>, ActionError>
    {
        val url = encode("projects?ids=${projectIdsToTypes.keys.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

        val responseString = this.requestProjectBody(url).getOrElse { return Err(it) }

        val response = json.decodeFromString<List<MrProjectModel>>(responseString)

        val fileIds = response.flatMap { it.versions }

        val projectFiles = requestMultipleProjectFiles(mcVersions, loaders, projectIdsToTypes, fileIds)
            .getOrElse { return Err(it) }

        val projects = response.mapNotNull { it.toProject().get() }

        projects.assignFiles(projectFiles, this)

        return Ok(projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet())
    }

    suspend fun requestMultipleProjectsWithFilesFromHashes(
        hashes: List<String>, algorithm: String
    ): Result<MutableSet<Project>, ActionError>
    {
        val responseString = this.requestProjectBody("version_files") {
            Json.encodeToString(GetVersionsFromHashesRequest(hashes, algorithm))
        }.getOrElse { return Err(it) }

        val response = json.decodeFromString<Map<String, MrVersionModel>>(responseString).values

        val projectFiles = response.flatMap { version -> version.toProjectFiles().take(1) }
        val projectIds = projectFiles.map { it.parentId }
        val projects = requestMultipleProjects(projectIds)
            .getOrElse { return Err(it) }

        projects.assignFiles(projectFiles, this)

        return Ok(projects)
    }

    suspend fun requestMultipleProjectFilesFromHashes(
        hashes: List<String>, algorithm: String
    ): Result<MutableSet<ProjectFile>, ActionError>
    {
        val responseString = this.requestProjectBody("version_files") {
            Json.encodeToString(GetVersionsFromHashesRequest(hashes, algorithm))
        }.getOrElse { return Err(it) }

        return Ok(json.decodeFromString<Map<String, MrVersionModel>>(responseString).values
            .flatMap { version -> version.toProjectFiles().take(1) }
            .toMutableSet())
    }
}