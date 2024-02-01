package teksturepako.pakku.api.platforms

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.GetVersionsFromHashesRequest
import teksturepako.pakku.api.models.MrProjectModel
import teksturepako.pakku.api.models.MrVersionModel
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.debugIfEmpty
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object Modrinth : Platform(
    name = "Modrinth",
    serialName = "modrinth",
    shortName = "mr",
    apiUrl = "https://api.modrinth.com",
    apiVersion = 2,
    url = "https://modrinth.com/mod"
)
{
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
                    println(json.decodeFromString<JsonObject>(this.body())["description"].finalize())
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
        if (requestsRemaining < 200 && requestsRemaining != 0) println("Rate limit remaining: $requestsRemaining")
    }

    // -- PROJECT --

    override suspend fun requestProject(input: String): Project? = when
    {
        input.matches("[0-9]{6}".toRegex()) -> null
        input.matches("\b[0-9a-zA-Z]{8}\b".toRegex()) -> requestProjectFromId(input)
        else -> requestProjectFromSlug(input)
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
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type $projectType not found!") }
            },
            id = mutableMapOf(serialName to id),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        return json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$id") ?: return null
        ).toProject()
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        return json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$slug") ?: return null
        ).toProject()
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        return json.decodeFromString<List<MrProjectModel>>(
            this.requestProjectBody("projects?ids=${
                ids.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }".replace(" ", "")) ?: return mutableSetOf()
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
                    loaders.any { loader -> loader == it } || it in validLoaders // Check default valid loaders
                } ?: true // If no loaders found, accept model
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
                url = versionFile.url,
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
            )
        }
    }

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        return if (fileId == null)
        {
            // Multiple files
            json.decodeFromString<List<MrVersionModel>>(
                this.requestProjectBody("project/$projectId/version") ?: return mutableSetOf()
            )
                .filterFileModels(mcVersions, loaders)
                .flatMap { version -> version.toProjectFiles().asReversed() }
                .debugIfEmpty {
                    println("${this::class.simpleName}#requestProjectFilesFromId: file is null")
                }.toMutableSet()
        } else
        {
            // One file
            json.decodeFromString<MrVersionModel>(
                this.requestProjectBody("version/$fileId") ?: return mutableSetOf()
            )
                .toProjectFiles().asReversed()
                .toMutableSet()
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>
    ): MutableSet<ProjectFile>
    {
        /* Chunk requests if there are too many ids */
        return ids.chunked(2_000).flatMap { list ->
            val url = "versions?ids=${
                list.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }".replace(" ", "")

            json.decodeFromString<List<MrVersionModel>>(
                this.requestProjectBody(url) ?: return mutableSetOf()
            )
                .filterFileModels(mcVersions, loaders)
                .flatMap { version -> version.toProjectFiles() }
        }
            .asReversed()
            .toMutableSet()
    }

    override suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        val response = json.decodeFromString<List<MrProjectModel>>(
            this.requestProjectBody("projects?ids=${
                ids.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }".replace(" ", "")) ?: return mutableSetOf()
        )

        val fileIds = response.flatMap { it.versions }
        val projectFiles = requestMultipleProjectFiles(mcVersions, loaders, fileIds)
        val projects = response.mapNotNull { it.toProject() }

        projects.assignFiles(projectFiles, this)

        return projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet()
    }

    suspend fun requestMultipleProjectsWithFilesFromHashes(
        hashes: List<String>, algorithm: String
    ): MutableSet<Project>
    {
        val response = json.decodeFromString<Map<String, MrVersionModel>>(
            this.requestProjectBody("version_files", GetVersionsFromHashesRequest(hashes, algorithm))
                ?: return mutableSetOf()
        ).values

        val projectFiles = response.flatMap { version -> version.toProjectFiles().asReversed().take(1) }
        val projectIds = projectFiles.map { it.parentId }
        val projects = requestMultipleProjects(projectIds)

        projects.assignFiles(projectFiles, this)

        return projects
    }
}