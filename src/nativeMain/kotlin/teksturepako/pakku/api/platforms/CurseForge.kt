package teksturepako.pakku.api.platforms

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.*
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.debug
import teksturepako.pakku.debugIfEmpty
import teksturepako.pakku.io.getEnv

@Suppress("MemberVisibilityCanBePrivate")
object CurseForge : Platform(
    name = "CurseForge",
    serialName = "curseforge",
    shortName = "cf",
    apiUrl = "https://cfproxy.bmpm.workers.dev",
    apiVersion = 1,
    url = "https://www.curseforge.com/minecraft/mc-mods"
)
{
    // -- API KEY --

    private const val API_KEY_HEADER = "x-api-key"
    private const val TEST_URL = "https://api.curseforge.com/v1/games"

    suspend fun getApiKey(): String?
    {
        return if (hasValidApiKey()) getEnv("CURSEFORGE_API_KEY") else null
    }

    private suspend fun hasValidApiKey(): Boolean
    {
        return isApiKeyValid(getEnv("CURSEFORGE_API_KEY") ?: return false)
    }

    private suspend fun isApiKeyValid(apiKey: String): Boolean
    {
        return super.requestBody(TEST_URL, Pair(API_KEY_HEADER, apiKey)).toString().isNotBlank()
    }

    override suspend fun requestBody(url: String): String?
    {
        return getApiKey()?.let { super.requestBody(url, Pair(API_KEY_HEADER, it)) } ?: super.requestBody(url)
    }

    // -- PROJECT --

    override suspend fun requestProject(input: String): Project? = when
    {
        input.matches("[0-9]{5,6}".toRegex()) -> requestProjectFromId(input)
        else                                  -> requestProjectFromSlug(input)
    }

    private fun CfModModel.toProject(): Project?
    {
        return Project(
            name = mutableMapOf(serialName to name),
            slug = mutableMapOf(serialName to slug),
            type = when (classId)
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                17   -> ProjectType.WORLD
                6552 -> ProjectType.SHADER

                else -> return null.also { println("Project type $classId not found!") }
            },
            id = mutableMapOf(serialName to id.toString()),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        return json.decodeFromString<GetProjectResponse>(
            this.requestProjectBody("mods/$id") ?: return null
        ).data.toProject()
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        return json.decodeFromString<SearchProjectResponse>(
            this.requestProjectBody("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug")
                ?: return null
        ).data.firstOrNull()?.toProject()
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        return json.decodeFromString<GetMultipleProjectsResponse>(
            this.requestProjectBody("mods", MultipleProjectsRequest(ids.map(String::toInt)))
                ?: return mutableSetOf()
        ).data.mapNotNull { it.toProject() }.toMutableSet()
    }

    // -- FILES --

    private const val LOADER_VERSION_TYPE_ID = 68441

    private fun List<CfModModel.File>.filterFileModels(
        mcVersions: List<String>, loaders: List<String>
    ): List<CfModModel.File> = this
        .filter { file ->
            file.gameVersions.any { it in mcVersions } && file.sortableGameVersions
                .filter { it.gameVersionTypeId == LOADER_VERSION_TYPE_ID } // Filter to loader only
                .takeIf { it.isNotEmpty() }
                ?.map { it.gameVersionName.lowercase() }?.any {
                    loaders.any { loader -> loader == it } || it in validLoaders // Check default valid loaders
                } ?: true // If no loaders found, accept model
        }

    private fun CfModModel.File.toProjectFile(gameVersionTypeIds: List<Int>): ProjectFile
    {
        return ProjectFile(
            type = this@CurseForge.serialName,
            fileName = this.fileName,
            mcVersions = this.sortableGameVersions
                .filter { it.gameVersionTypeId in gameVersionTypeIds }
                .map { it.gameVersionName }.toMutableList(),
            loaders = this.sortableGameVersions
                .filter { it.gameVersionTypeId == LOADER_VERSION_TYPE_ID } // Filter to loader only
                .map { it.gameVersionName.lowercase() }.toMutableList(),
            releaseType = when (this.releaseType)
            {
                1    -> "release"
                2    -> "beta"
                3    -> "alpha"

                else -> "release"
            },
            url = this.downloadUrl?.replace(" ", "%20"), // Replace empty characters in the URL
            id = this.id.toString(),
            parentId = this.modId.toString(),
            hashes = this.hashes.associate {
                when (it.algo)
                {
                    1 -> "sha1" to it.value
                    else -> "md5" to it.value
                }
            }.toMutableMap(),
            requiredDependencies = this.dependencies
                .filter { it.relationType == 3 }
                .map { it.modId.toString() }.toMutableSet(),
            size = this.fileLength,
        )
    }

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        // Handle optional fileId
        val fileIdSuffix = if (fileId == null) "" else "/$fileId"

        // Prepare base request URL
        var requestUrl = "mods/$projectId/files$fileIdSuffix?"

        // Handle mcVersions
        val gameVersionTypeIds = mcVersions.mapNotNull { version: String ->
            requestGameVersionTypeId(version)?.also {
                requestUrl += "&gameVersionTypeId=$it"
            }
        }

        // Handle loaders
        requestUrl += "&modLoaderTypes=${loaders.joinToString(",")}"

        return if (fileId == null)
        {
            // Multiple files
            json.decodeFromString<GetMultipleFilesResponse>(
                this.requestProjectBody(requestUrl) ?: return mutableSetOf()
            ).data
                .filterFileModels(mcVersions, loaders)
                .map { it.toProjectFile(gameVersionTypeIds).fetchAlternativeDownloadUrl() }
                .debugIfEmpty {
                    println("${this::class.simpleName}#requestProjectFiles: file is null")
                }
                .toMutableSet()
        } else
        {
            // One file
            mutableSetOf(
                json.decodeFromString<GetFileResponse>(
                    this.requestProjectBody(requestUrl) ?: return mutableSetOf()
                ).data
                    .toProjectFile(gameVersionTypeIds)
                    .fetchAlternativeDownloadUrl()
            )
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>
    ): MutableSet<ProjectFile>
    {
        // Handle mcVersions
        val gameVersionTypeIds = mcVersions.mapNotNull { version: String ->
            requestGameVersionTypeId(version)
        }

        return json.decodeFromString<GetMultipleFilesResponse>(
            this.requestProjectBody("mods/files", MultipleFilesRequest(ids.map(String::toInt))) ?: return mutableSetOf()
        ).data
            .filterFileModels(mcVersions, loaders)
            .sortedByDescending { it.fileDate }
            .map {
                it.toProjectFile(gameVersionTypeIds).fetchAlternativeDownloadUrl()
            }
            .debugIfEmpty {
                println("${this::class.simpleName}#requestMultipleProjectFiles: file is null")
            }
            .toMutableSet()
    }

    override suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        val response = json.decodeFromString<GetMultipleProjectsResponse>(
            this.requestProjectBody("mods", MultipleProjectsRequest(ids.map(String::toInt)))
                ?: return mutableSetOf()
        ).data

        val fileIds = response.flatMap { model ->
            model.latestFilesIndexes.map { it.fileId.toString() }
        }

        val projectFiles = requestMultipleProjectFiles(mcVersions, loaders, fileIds)

        val projects = response.mapNotNull { it.toProject() }

        projects.assignFiles(projectFiles, this)

        return projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet()
    }

    suspend fun requestGameVersionTypeId(mcVersion: String): Int?
    {
        return json.decodeFromString<JsonObject>(
            this.requestProjectBody("minecraft/version/$mcVersion") ?: return null
        )["data"]!!.jsonObject["gameVersionTypeId"].toString().toInt()
            .debug { println("${this::class.simpleName}#requestGameVersionTypeId: $it") }
    }

    fun ProjectFile.fetchAlternativeDownloadUrl(): ProjectFile =
        if (this.url != "null" && this.url != null) this else
        {
            val url = fetchAlternativeDownloadUrl(this.id, this.fileName)
            this.apply {
                // Replace empty characters in the URL.
                this.url = url.replace(" ", "%20")
            }
        }

    fun fetchAlternativeDownloadUrl(fileId: String, fileName: String): String =
        "https://edge.forgecdn.net/files/${fileId.substring(0, 4)}/${fileId.substring(4)}/$fileName"
}