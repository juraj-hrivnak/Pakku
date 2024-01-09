package teksturepako.pakku.api.platforms

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.MrProjectModel
import teksturepako.pakku.api.models.MrVersionModel
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debugIfEmpty
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object Modrinth : Platform(
    name = "Modrinth",
    serialName = "modrinth",
    apiUrl = "https://api.modrinth.com",
    apiVersion = 2
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
        if (requestsRemaining > 0) println("Rate limit remaining: $requestsRemaining")
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
            name = mutableMapOf(serialName to this.title),
            slug = mutableMapOf(serialName to this.slug),
            type = when (this.projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type ${this.projectType} not found!") }
            },
            id = mutableMapOf(serialName to this.id),
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
        return this.files.map { versionFile ->
            MrFile(
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
                    .mapNotNull { it.projectId }.toMutableSet()
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
                .flatMap { version -> version.toProjectFiles() }
                .debugIfEmpty {
                    println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
                }.toMutableSet()
        } else
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
        mcVersions: List<String>, loaders: List<String>, ids: List<String>
    ): MutableSet<ProjectFile>
    {
        return json.decodeFromString<List<MrVersionModel>>(
            this.requestProjectBody("versions?ids=${
                ids.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }".replace(" ", "")) ?: return mutableSetOf()
        )
            .filterFileModels(mcVersions, loaders)
            .flatMap { version -> version.toProjectFiles() }
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

        projects.forEach { project ->
            projectFiles.forEach { projectFile ->
                if (project.id[this.serialName] == projectFile.parentId)
                {
                    project.files.add(projectFile)
                }
            }
        }

        return projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet()
    }
}