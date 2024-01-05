package teksturepako.pakku.api.platforms

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.*
import teksturepako.pakku.api.projects.CfFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug

@Suppress("MemberVisibilityCanBePrivate")
object CurseForge : Platform(
    name = "CurseForge",
    serialName = "curseforge",
    apiUrl = "https://cfproxy.bmpm.workers.dev",
    apiVersion = 1
) {
    // -- API KEY --

    private const val API_KEY_HEADER = "x-api-key"
    private const val TEST_URL = "https://api.curseforge.com/v1/games"

    suspend fun getApiKey(): String?
    {
        return if (hasValidApiKey()) System.getenv("CURSEFORGE_API_KEY") ?: null else null
    }

    private suspend fun hasValidApiKey(): Boolean
    {
        return isApiKeyValid(System.getenv("CURSEFORGE_API_KEY") ?: return false)
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
        input.matches("[0-9]{6}".toRegex()) -> requestProjectFromId(input)
        else -> requestProjectFromSlug(input)
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        val response = json.decodeFromString<GetProjectResponse>(
            this.requestProjectBody("mods/$id")
                ?: return null
        ).data

        return Project(
            name = mutableMapOf(serialName to response.name),
            slug = mutableMapOf(serialName to response.slug),
            type = when (response.classId)
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                17   -> ProjectType.WORLD
                6552 -> ProjectType.SHADER

                else -> return null.also { println("Project type ${response.classId} not found!") }
            },
            id = mutableMapOf(serialName to response.id.toString()),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val response = json.decodeFromString<SearchProjectResponse>(
            this.requestProjectBody("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug")
                ?: return null
        ).data.firstOrNull() ?: return null

        return Project(
            name = mutableMapOf(serialName to response.name),
            slug = mutableMapOf(serialName to response.slug),
            type = when (response.classId)
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                17   -> ProjectType.WORLD
                6552 -> ProjectType.SHADER

                else -> return null.also { println("Project type ${response.classId} not found!") }
            },
            id = mutableMapOf(serialName to response.id.toString()),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        val response = json.decodeFromString<GetMultipleProjectsResponse>(
            this.requestProjectBody("mods", MultipleProjectsRequest(ids.map(String::toInt)))
                ?: return mutableSetOf()
        ).data

        return response.map { project ->
            Project(
                name = mutableMapOf(serialName to project.name),
                slug = mutableMapOf(serialName to project.slug),
                type = when (project.classId)
                {
                    6    -> ProjectType.MOD
                    12   -> ProjectType.RESOURCE_PACK
                    17   -> ProjectType.WORLD
                    6552 -> ProjectType.SHADER

                    else -> ProjectType.MOD.also { println("Project type ${project.classId} not found!") }
                },
                id = mutableMapOf(serialName to project.id.toString()),
                files = mutableSetOf(),
            )
        }.toMutableSet()
    }

    // -- FILES --

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        val loaderVersionTypeId = 68441

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

        return if (fileId == null) // Multiple files
        {
            val response = json.decodeFromString<GetMultipleFilesResponse>(
                this.requestProjectBody(requestUrl) ?: return mutableSetOf()
            ).data

            response.filter { file ->
                file.gameVersions.any { it in mcVersions } && file.sortableGameVersions
                    .filter { it.gameVersionTypeId == loaderVersionTypeId /* Filter to loader only */ }
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.gameVersionName.lowercase() }?.any {
                        loaders.any { loader -> loader == it } || it in listOf(
                            "minecraft", "iris", "optifine", "datapack" // Loaders valid by default
                        )
                    } ?: true
            }.map { file ->
                CfFile(
                    fileName = file.fileName,
                    mcVersions = file.sortableGameVersions
                        .filter { it.gameVersionTypeId in gameVersionTypeIds }
                        .map { it.gameVersionName }.toMutableList(),
                    loaders = file.sortableGameVersions
                        .filter { it.gameVersionTypeId == 68441 /* Filter to loader only */ }
                        .map { it.gameVersionName.lowercase() }.toMutableList(),
                    releaseType = when (file.releaseType)
                    {
                        1 -> "release"
                        2 -> "beta"
                        3 -> "alpha"

                        else -> "release"
                    },
                    url = file.downloadUrl?.replace(" ", "%20"), // Replace empty characters in the URL
                    id = file.id.toString(),
                    parentId = file.modId.toString(),
                    requiredDependencies = file.dependencies
                        .filter { it.relationType == 3 }
                        .map { it.modId.toString() }.toMutableSet()
                )
            }.debug {
                if (it.isEmpty()) println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
            }.map { file ->
                // Request alternative download URLs.
                if (file.url != "null") file else
                {
                    val url = fetchAlternativeDownloadUrl(file.id, file.fileName)
                    file.apply {
                        // Replace empty characters in the URL.
                        this.url = url.replace(" ", "%20")
                    }
                }
            }.toMutableSet()
        } else // One file
        {
            val response = json.decodeFromString<GetFileResponse>(
                this.requestProjectBody(requestUrl) ?: return mutableSetOf()
            ).data

            mutableSetOf(
                CfFile(
                    fileName = response.fileName,
                    mcVersions = response.sortableGameVersions
                        .filter { it.gameVersionTypeId in gameVersionTypeIds }
                        .map { it.gameVersionName }.toMutableList(),
                    loaders = response.sortableGameVersions
                        .filter { it.gameVersionTypeId == 68441 /* Filter to loader only */ }
                        .map { it.gameVersionName.lowercase() }.toMutableList(),
                    releaseType = when (response.releaseType)
                    {
                        1 -> "release"
                        2 -> "beta"
                        3 -> "alpha"

                        else -> "release"
                    },
                    url = response.downloadUrl?.replace(" ", "%20"), // Replace empty characters in the URL
                    id = response.id.toString(),
                    parentId = response.modId.toString(),
                    requiredDependencies = response.dependencies
                        .filter { it.relationType == 3 }
                        .map { it.modId.toString() }.toMutableSet()
                ).let { file ->
                    if (file.url != "null") file else
                    {
                        val url = fetchAlternativeDownloadUrl(file.id, file.fileName)
                        file.apply {
                            // Replace empty characters in the URL.
                            this.url = url.replace(" ", "%20")
                        }
                    }
                }
            )
        }
    }

    override suspend fun requestMultipleProjectFiles(ids: List<String>): MutableSet<ProjectFile>
    {
        val response = json.decodeFromString<GetMultipleFilesResponse>(
            this.requestProjectBody("files", MultipleFilesRequest(ids.map(String::toInt)))
                ?: return mutableSetOf()
        ).data

        return response.map { file ->
            CfFile(
                fileName = file.fileName,
                mcVersions = file.sortableGameVersions
                    .map { it.gameVersionName }.toMutableList(),
                loaders = file.sortableGameVersions
                    .filter { it.gameVersionTypeId == 68441 /* Filter to loader only */ }
                    .map { it.gameVersionName.lowercase() }.toMutableList(),
                releaseType = when (file.releaseType)
                {
                    1 -> "release"
                    2 -> "beta"
                    3 -> "alpha"

                    else -> "release"
                },
                url = file.downloadUrl?.replace(" ", "%20"), // Replace empty characters in the URL
                id = file.id.toString(),
                parentId = file.modId.toString(),
                requiredDependencies = file.dependencies
                    .filter { it.relationType == 3 }
                    .map { it.modId.toString() }.toMutableSet()
            )
        }.debug {
            if (it.isEmpty()) println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
        }.map { file ->
            // Request alternative download URLs.
            if (file.url != "null") file else
            {
                val url = fetchAlternativeDownloadUrl(file.id, file.fileName)
                file.apply {
                    // Replace empty characters in the URL.
                    this.url = url.replace(" ", "%20")
                }
            }
        }.toMutableSet()
    }

    suspend fun requestGameVersionTypeId(mcVersion: String): Int?
    {
        return json.decodeFromString<JsonObject>(
            this.requestProjectBody("minecraft/version/$mcVersion") ?: return null
        )["data"]!!.jsonObject["gameVersionTypeId"].toString().toInt()
            .debug { println("${this.javaClass.simpleName}#requestGameVersionTypeId: $it") }
    }

    fun fetchAlternativeDownloadUrl(fileId: String, fileName: String): String
    {
        return "https://edge.forgecdn.net/files/${fileId.substring(0, 4)}/${
            fileId.toString().substring(4)
        }/$fileName"
    }
}