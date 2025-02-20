package teksturepako.pakku.api.platforms

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import net.thauvin.erik.urlencoder.UrlEncoderUtil.encode
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.mr.GetVersionsFromHashesRequest
import teksturepako.pakku.api.models.mr.MrProjectModel
import teksturepako.pakku.api.models.mr.MrVersionModel
import teksturepako.pakku.api.projects.*
import teksturepako.pakku.debugIfEmpty
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

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

    // -- API RATE LIMIT --

    private var requestsRemaining = 0
    private var waiting = false

    override suspend fun HttpResponse.checkLimit(): HttpResponse
    {
        this.headers["x-ratelimit-remaining"]?.toInt()?.let { rateLimit ->
            requestsRemaining = rateLimit
            when
            {
                rateLimit == 0  ->
                {
                    print("Error: ")
                    println(json.decodeFromString<JsonObject>(this.body())["description"])
                    exitProcess(1)
                }
                rateLimit < 100 ->
                {
                    if (!waiting) println("Warning: Waiting 30 seconds, because of height Modrinth API usage")
                    waiting = true
                    delay(30.seconds)
                }
            }
        }
        return this
    }

    fun checkRateLimit()
    {
        if (requestsRemaining < 200 && requestsRemaining != 0)
        {
            println("Modrinth: Rate limit remaining: $requestsRemaining")
        }
    }

    // -- PROJECT --

    override suspend fun requestProject(input: String, projectType: ProjectType?): Project?
    {
        return when
        {
            input.matches("[0-9]{6}".toRegex())           -> null
            input.matches("\b[0-9a-zA-Z]{8}\b".toRegex()) ->
            {
                json.decodeFromString<MrProjectModel>(
                    this.requestProjectBody("project/$input") ?: return null
                ).toProject()
            }
            else                                          ->
            {
                json.decodeFromString<MrProjectModel>(
                    this.requestProjectBody("project/$input") ?: return null
                ).toProject()
            }
        }.also { project -> projectType?.let { project?.type = it } }
    }

    private fun MrProjectModel.toProject(): Project?
    {
        return Project(
            name = mutableMapOf(serialName to title),
            slug = mutableMapOf(serialName to slug),
            type = when (projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "datapack"     -> ProjectType.DATA_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type $projectType not found!") }
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
        )
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        val url = encode("projects?ids=${ids.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

        return json.decodeFromString<List<MrProjectModel>>(
            this.requestProjectBody(url) ?: return mutableSetOf()
        ).mapNotNull { it.toProject() }.toMutableSet()
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
    ): MutableSet<ProjectFile>
    {
        val actualLoaders = when (projectType)
        {
            ProjectType.DATA_PACK -> listOf(DATAPACK_LOADER)
            else                  -> loaders
        }

        return if (fileId == null)
        {
            // Multiple files
            json.decodeFromString<List<MrVersionModel>>(
                this.requestProjectBody("project/$projectId/version") ?: return mutableSetOf()
            )
                .filterFileModels(mcVersions, actualLoaders)
                .sortedWith(compareBy(compareByLoaders(actualLoaders)))
                .flatMap { version -> version.toProjectFiles() }
                .debugIfEmpty {
                    println("${this::class.simpleName}#requestProjectFiles: file is null")
                }
                .toMutableSet()
        }
        else
        {
            // One file
            json.decodeFromString<MrVersionModel>(
                this.requestProjectBody("version/$fileId") ?: return mutableSetOf()
            )
                .toProjectFiles()
                .toMutableSet()
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, ids: List<String>
    ): MutableSet<ProjectFile> = coroutineScope {
        val loadersWithType =
            (if (projectIdsToTypes.values.any { it == ProjectType.DATA_PACK }) listOf(DATAPACK_LOADER) else listOf()) + loaders
        // Chunk requests if there are too many ids; Also do this in parallel
        return@coroutineScope ids.chunked(1_000).map { list ->
            async {
                val url = encode("versions?ids=${list.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

                json.decodeFromString<List<MrVersionModel>>(
                    this@Modrinth.requestProjectBody(url) ?: return@async mutableSetOf()
                )
            }
        }
            .awaitAll()
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
    }

    override suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        val url = encode("projects?ids=${projectIdsToTypes.keys.map { "\"$it\"" }}".filterNot { it.isWhitespace() }, allow = "?=")

        val response = json.decodeFromString<List<MrProjectModel>>(
            this.requestProjectBody(url) ?: return mutableSetOf()
        )

        val fileIds = response.flatMap { it.versions }
        val projectFiles = requestMultipleProjectFiles(mcVersions, loaders, projectIdsToTypes, fileIds)
        val projects = response.mapNotNull { it.toProject() }

        projects.assignFiles(projectFiles, this)

        return projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet()
    }

    suspend fun requestMultipleProjectsWithFilesFromHashes(
        hashes: List<String>, algorithm: String
    ): MutableSet<Project>
    {
        val response = json.decodeFromString<Map<String, MrVersionModel>>(
            this.requestProjectBody("version_files") {
                Json.encodeToString(GetVersionsFromHashesRequest(hashes, algorithm))
            } ?: return mutableSetOf()
        ).values

        val projectFiles = response.flatMap { version -> version.toProjectFiles().take(1) }
        val projectIds = projectFiles.map { it.parentId }
        val projects = requestMultipleProjects(projectIds)

        projects.assignFiles(projectFiles, this)

        return projects
    }

    suspend fun requestMultipleProjectFilesFromHashes(
        hashes: List<String>, algorithm: String
    ): MutableSet<ProjectFile>
    {
        return json.decodeFromString<Map<String, MrVersionModel>>(
            this.requestProjectBody("version_files") {
                Json.encodeToString(GetVersionsFromHashesRequest(hashes, algorithm))
            } ?: return mutableSetOf()
        ).values
            .flatMap { version -> version.toProjectFiles().take(1) }
            .toMutableSet()
    }
}