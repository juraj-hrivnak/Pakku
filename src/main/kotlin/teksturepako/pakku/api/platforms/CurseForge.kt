package teksturepako.pakku.api.platforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.CfFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

@Suppress("MemberVisibilityCanBePrivate")
object CurseForge : Platform()
{
    override val name = "CurseForge"
    override val serialName = "curseforge"
    override val apiUrl = "https://cfproxy.bmpm.workers.dev"
    override val apiVersion = 1

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

    override suspend fun requestProjectFromId(id: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectBody("mods/$id") ?: return null)

        return Project(
            name = mutableMapOf(serialName to json["data"]!!.jsonObject["name"].finalize()),
            slug = mutableMapOf(serialName to json["data"]!!.jsonObject["slug"].finalize()),
            type = when (json["data"]!!.jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                17   -> ProjectType.WORLD
                6552 -> ProjectType.SHADER

                else -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(serialName to json["data"]!!.jsonObject["id"].finalize()),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(
            this.requestProjectBody("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug")
                ?: return null
        )
        if (json["data"]!!.jsonArray.isEmpty()) return null

        return Project(
            name = mutableMapOf(serialName to json["data"]!!.jsonArray.first().jsonObject["name"].finalize()),
            slug = mutableMapOf(serialName to json["data"]!!.jsonArray.first().jsonObject["slug"].finalize()),
            type = when (json["data"]!!.jsonArray.first().jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                17   -> ProjectType.WORLD
                6552 -> ProjectType.SHADER

                else -> return null.debug {
                    println(
                        "Project type not found for $slug: ${
                            (json["data"]!!.jsonArray.first().jsonObject["classId"]!!.toString().toInt())
                        }"
                    )
                }
            },
            id = mutableMapOf(serialName to json["data"]!!.jsonArray.first().jsonObject["id"].finalize()),
            files = mutableSetOf(),
        )
    }

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param fileId The file ID.
     * @return A mutable list of [CfFile] objects, or null if an error occurs or no files are found.
     */
    override suspend fun requestProjectFilesFromFileId(
        mcVersion: String, loader: String, fileId: String
    ): MutableSet<ProjectFile>?
    {
        val gameVersionTypeId = requestGameVersionTypeId(mcVersion)
        // TODO: better version handling?
        val requestUrl = "mods/$fileId/files?gameVersionTypeId=$gameVersionTypeId&modLoaderType=$loader"

        val data: JsonObject = json.decodeFromString(this.requestProjectBody(requestUrl) ?: return null
            .debug {println("Error ${this.toPrettyString()}#val data = null") }
        )

        if (data["data"]!!.jsonArray.isEmpty()) return null
            .debug { println("Error ${this.toPrettyString()}#data is empty") }

        return data["data"]!!.jsonArray.filter { file ->
            mcVersion in json.decodeFromJsonElement<List<String>>(file.jsonObject["gameVersions"]!!)
                    && json.decodeFromJsonElement<List<SortableVersion>>(file.jsonObject["sortableGameVersions"]!!)
                        .filter { it.gameVersionTypeId == 68441 /* Filter to loader only */ }
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.gameVersionName.lowercase() }
                        ?.any {
                            loader == it || it in listOf("minecraft", "iris", "optifine", "datapack")
                        } ?: true
        }.map { file ->
            CfFile(
                fileName = file.jsonObject["fileName"].finalize(),
                mcVersions = json.decodeFromJsonElement<List<SortableVersion>>(file.jsonObject["sortableGameVersions"]!!)
                    .filter { it.gameVersionTypeId == gameVersionTypeId }
                    .map { it.gameVersionName }
                    .toMutableList(),
                loaders = json.decodeFromJsonElement<List<SortableVersion>>(file.jsonObject["sortableGameVersions"]!!)
                    .filter { it.gameVersionTypeId == 68441 /* Filter to loader only */ }
                    .map { it.gameVersionName.lowercase() }
                    .toMutableList(),
                releaseType = when (file.jsonObject["releaseType"].toString().toInt())
                {
                    1    -> "release"
                    2    -> "beta"
                    3    -> "alpha"

                    else -> "release"
                },
                url = file.jsonObject["downloadUrl"].finalize().replace(" ", "%20"),
                id = file.jsonObject["id"].toString().toInt(),
                requiredDependencies = file.jsonObject["dependencies"]?.jsonArray
                    ?.filter { it.jsonObject["relationType"].toString().toInt() == 3 }
                    ?.map { it.jsonObject["modId"].finalize() }
                    ?.toMutableSet()
            )
        }.debug { if (it.isEmpty()) println("Error ${this.toPrettyString()}#project file is null") }.toMutableSet()
    }

    @Serializable
    data class SortableVersion(
        val gameVersionName: String, val gameVersionTypeId: Int
    )

    suspend fun requestGameVersionTypeId(mcVersion: String): Int
    {
        return json.decodeFromString<JsonObject>(this.requestProjectBody("minecraft/version/$mcVersion")
            ?: return 0.debug { println("Error $this#requestGameVersionTypeId") })["data"]!!.jsonObject["gameVersionTypeId"].toString()
            .toInt().debug { println("$this#requestGameVersionTypeId: $it") }
    }

    suspend fun requestUrl(modId: Int, fileId: Int): String?
    {
        return json.decodeFromString<JsonObject>(
            this.requestProjectBody("mods/$modId/files/$fileId/download-url") ?: return null
        )["data"].finalize()
    }

    fun fetchAlternativeDownloadUrl(fileId: Int, fileName: String): String
    {
        return "https://edge.forgecdn.net/files/" + "${fileId.toString().substring(0, 4)}/${
            fileId.toString().substring(4)
        }/$fileName"
    }

    override suspend fun requestFilesForProject(
        mcVersions: List<String>, loaders: List<String>, project: Project, numberOfFiles: Int
    ): MutableSet<ProjectFile>
    {
        val result = mutableSetOf<ProjectFile>()
        project.id[this.serialName]?.let { projectId ->
            requestProjectFilesFromFileId(mcVersions, loaders, projectId).take(numberOfFiles)
                .filterIsInstance<CfFile>()
                .forEach { file ->
                    // Request URL if is null and add to project files.
                    if (file.url != "null") result.add(file) else
                    {
                        val url = fetchAlternativeDownloadUrl(file.id, file.fileName)
                        result.add(file.apply {
                            // Replace empty characters in the URL.
                            this.url = url.replace(" ", "%20")
                        })
                    }
                }
        }
        return result
    }
}