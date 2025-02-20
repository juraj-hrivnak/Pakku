package teksturepako.pakku.api.platforms

import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.thauvin.erik.urlencoder.UrlEncoderUtil.decode
import teksturepako.pakku.api.PakkuApi
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.cf.*
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.debug
import teksturepako.pakku.debugIfEmpty
import teksturepako.pakku.io.toMurmur2

@Suppress("MemberVisibilityCanBePrivate")
object CurseForge : Platform(
    name = "CurseForge",
    serialName = "curseforge",
    shortName = "cf",
    apiUrl = "https://api.curseforge.com",
    apiVersion = 1,
    siteUrl = "https://www.curseforge.com/minecraft"
)
{
    // -- URLS --

    override fun getUrlForProjectType(projectType: ProjectType): String = when (projectType)
    {
        ProjectType.MOD           -> "${this.siteUrl}/mc-mods"
        ProjectType.RESOURCE_PACK -> "${this.siteUrl}/texture-packs"
        ProjectType.DATA_PACK     -> "${this.siteUrl}/data-packs"
        ProjectType.WORLD         -> "${this.siteUrl}/worlds"
        ProjectType.SHADER        -> "${this.siteUrl}/shaders"
    }

    // -- API KEY --

    private const val API_KEY_HEADER = "x-api-key"

    private val apiKey: String? = PakkuApi.curseForgeApiKey?.takeIf { it.isNotBlank() }

    fun checkApiKey()
    {
        if (apiKey != null) println("CurseForge: Using embedded API key.")
    }

    override fun getCommonRequestUrl(apiUrl: String, apiVersion: Int): String
    {
        // In dev environment
        return if (apiKey == null) super.getCommonRequestUrl("https://cfproxy.bmpm.workers.dev", apiVersion)
        else super.getCommonRequestUrl(apiUrl, apiVersion)
    }

    override suspend fun requestBody(url: String): String?
    {
        return if (apiKey == null) super.requestBody(url)
        else super.requestBody(url, Pair(API_KEY_HEADER, apiKey))
    }

    override suspend fun requestBody(url: String, bodyContent: () -> String): String?
    {
        return if (apiKey == null) super.requestBody(url, bodyContent)
        else super.requestBody(url, bodyContent, Pair(API_KEY_HEADER, apiKey))
    }

    // -- PROJECT --

    override suspend fun requestProject(input: String, projectType: ProjectType?): Project? = when
    {
        input.matches("[0-9]{5,7}".toRegex()) -> requestProjectFromId(input)
        else                                  -> requestProjectFromSlug(input)
    }.also { project -> projectType?.let { project?.type = it } }

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
                6945 -> ProjectType.DATA_PACK

                else -> return null.also { println("$slug: Project type $classId from CurseForge isn't supported") }
            },
            id = mutableMapOf(serialName to id.toString()),
            redistributable = allowModDistribution ?: isAvailable,
            files = mutableSetOf(),
        )
    }

    suspend fun requestProjectFromId(id: String): Project?
    {
        return json.decodeFromString<GetProjectResponse>(
            this.requestProjectBody("mods/$id") ?: return null
        ).data.toProject()
    }

    suspend fun requestProjectFromSlug(slug: String): Project?
    {
        return json.decodeFromString<SearchProjectResponse>(
            this.requestProjectBody("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug")
                ?: return null
        ).data.firstOrNull()?.toProject()
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        return json.decodeFromString<GetMultipleProjectsResponse>(
            this.requestProjectBody("mods") {
                Json.encodeToString(MultipleProjectsRequest(ids.map(String::toInt)))
            } ?: return mutableSetOf()
        ).data.mapNotNull { it.toProject() }.toMutableSet()
    }

    // -- FILES --

    internal const val LOADER_VERSION_TYPE_ID = 68441

    private fun List<CfModModel.File>.filterFileModels(
        mcVersions: List<String>, loaders: List<String>
    ): List<CfModModel.File> = this
        .filter { file ->
            file.gameVersions.any { it in mcVersions } && file.sortableGameVersions
                .filter { it.gameVersionTypeId == LOADER_VERSION_TYPE_ID } // Filter to loader only
                .takeIf { it.isNotEmpty() }
                ?.map { it.gameVersionName.lowercase() }?.any {
                    it in loaders || it in validLoaders // Check default valid loaders
                } ?: true // If no loaders found, accept model
        }

    internal fun compareByLoaders(loaders: List<String>): (CfModModel.File) -> Comparable<*> = { file: CfModModel.File ->
        val fileLoaders = file.sortableGameVersions.filter { it.gameVersionTypeId == LOADER_VERSION_TYPE_ID }
            .map { it.gameVersionName.lowercase() }
        loaders.indexOfFirst { it in fileLoaders }.let { if (it == -1) loaders.size else it }
    }

    private fun CfModModel.File.toProjectFile(gameVersionTypeIds: List<Int>): ProjectFile
    {
        return ProjectFile(
            type = this@CurseForge.serialName,
            fileName = this.fileName,
            mcVersions = this.sortableGameVersions
                // TODO: Rework this to only filter type IDs other than for mc versions.
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
            url = this.downloadUrl?.let { decode(it) }, // Decode URL
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
                .filter { it.relationType == 3 } // Filter to required dependency only
                .map { it.modId.toString() }.toMutableSet(),
            size = this.fileLength,
            datePublished = Instant.parse(fileDate)
        ).fetchAlternativeDownloadUrl()
    }

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?, projectType: ProjectType?
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
                .sortedWith(compareBy(compareByLoaders(loaders)))
                .map { it.toProjectFile(gameVersionTypeIds) }
                .debugIfEmpty {
                    println("${this::class.simpleName}#requestProjectFiles: file is null")
                }
                .toMutableSet()
        }
        else
        {
            // One file
            mutableSetOf(
                json.decodeFromString<GetFileResponse>(
                    this.requestProjectBody(requestUrl) ?: return mutableSetOf()
                ).data.toProjectFile(gameVersionTypeIds)
            )
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, ids: List<String>
    ): MutableSet<ProjectFile>
    {
        // Handle mcVersions
        val gameVersionTypeIds = mcVersions.mapNotNull { version: String ->
            requestGameVersionTypeId(version)
        }

        return json.decodeFromString<GetMultipleFilesResponse>(
            this.requestProjectBody("mods/files") {
                Json.encodeToString(MultipleFilesRequest(ids.map(String::toInt)))
            } ?: return mutableSetOf()
        ).data
            .filterFileModels(mcVersions, loaders)
            .sortedWith(compareByDescending<CfModModel.File> { Instant.parse(it.fileDate) }.thenBy(compareByLoaders(loaders)))
            .map { it.toProjectFile(gameVersionTypeIds) }
            .debugIfEmpty {
                println("${this::class.simpleName}#requestMultipleProjectFiles: file is null")
            }
            .toMutableSet()
    }

    override suspend fun requestMultipleProjectsWithFiles(
        mcVersions: List<String>, loaders: List<String>, projectIdsToTypes: Map<String, ProjectType?>, numberOfFiles: Int
    ): MutableSet<Project>
    {
        val response = json.decodeFromString<GetMultipleProjectsResponse>(
            this.requestProjectBody("mods") {
                Json.encodeToString(MultipleProjectsRequest(projectIdsToTypes.keys.map(String::toInt)))
            } ?: return mutableSetOf()
        ).data

        val fileIds = response.flatMap { model ->
            model.latestFilesIndexes.map { it.fileId.toString() }
        }

        val projectFiles = requestMultipleProjectFiles(mcVersions, loaders, projectIdsToTypes, fileIds)
        val projects = response.mapNotNull { it.toProject() }

        projects.assignFiles(projectFiles, this)

        return projects.map { it.apply { files = files.take(numberOfFiles).toMutableSet() } }.toMutableSet()
    }

    suspend fun requestMultipleProjectsWithFilesFromBytes(
        mcVersions: List<String>, bytes: List<ByteArray>
    ): MutableSet<Project>
    {
        // Handle mcVersions
        val gameVersionTypeIds = mcVersions.mapNotNull { version: String ->
            requestGameVersionTypeId(version)
        }

        val murmurs = bytes.map { it.toMurmur2() }

        val response = json.decodeFromString<GetFingerprintsMatchesResponse>(
            this.requestProjectBody("fingerprints/432") {
                Json.encodeToString(GetFingerprintsMatches(murmurs))
            } ?: return mutableSetOf()
        ).data.exactMatches

        val projectFiles = response
            .filter { it.file.isAvailable }
            .map { match -> match.file.toProjectFile(gameVersionTypeIds) }
        val projectIds = projectFiles.map { it.parentId }
        val projects = requestMultipleProjects(projectIds)

        projects.assignFiles(projectFiles, this)

        return projects
    }

    suspend fun requestMultipleProjectFilesFromBytes(
        mcVersions: List<String>, bytes: List<ByteArray>
    ): MutableSet<ProjectFile>
    {
        // Handle mcVersions
        val gameVersionTypeIds = mcVersions.mapNotNull { version: String ->
            requestGameVersionTypeId(version)
        }

        val murmurs = bytes.map { it.toMurmur2() }

        return json.decodeFromString<GetFingerprintsMatchesResponse>(
            this.requestProjectBody("fingerprints/432") {
                Json.encodeToString(GetFingerprintsMatches(murmurs))
            } ?: return mutableSetOf()
        ).data.exactMatches
            .filter { it.file.isAvailable }
            .map { match -> match.file.toProjectFile(gameVersionTypeIds) }
            .toMutableSet()
    }

    suspend fun requestGameVersionTypeId(mcVersion: String): Int?
    {
        return json.decodeFromString<JsonObject>(
            this.requestProjectBody("minecraft/version/$mcVersion") ?: return null
        )["data"]!!.jsonObject["gameVersionTypeId"].toString().toInt()
            .debug { println("${this::class.simpleName}#requestGameVersionTypeId: $it") }
    }

    fun ProjectFile.fetchAlternativeDownloadUrl(): ProjectFile =
        if (this.url != "null" && this.url != null) this else this.apply {
            this.url = fetchAlternativeDownloadUrl(this.id, this.fileName)
        }

    fun fetchAlternativeDownloadUrl(fileId: String, fileName: String): String =
        decode("https://edge.forgecdn.net/files/${fileId.substring(0, 4)}/${fileId.substring(4)}/$fileName")
}
