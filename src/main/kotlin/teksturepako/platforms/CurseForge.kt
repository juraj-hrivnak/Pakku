package teksturepako.platforms

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import teksturepako.data.finalize
import teksturepako.data.json
import teksturepako.projects.Project
import teksturepako.projects.ProjectFile
import teksturepako.projects.ProjectType

object CurseForge : Platform()
{
    override val name = "CurseForge"
    override val apiUrl = "https://cfproxy.bmpm.workers.dev"
    override val apiVersion = 1

    private const val API_KEY_HEADER = "x-api-key"
    private const val TEST_URL = "https://api.curseforge.com/v1/games"

    suspend fun getApiKey(): String?
    {
        return if (hasValidApiKey()) System.getenv("CURSEFORGE_API_KEY") ?: null else null
    }

    private suspend fun hasValidApiKey(): Boolean {
        return isApiKeyValid(System.getenv("CURSEFORGE_API_KEY") ?: return false)
    }

    private suspend fun isApiKeyValid(apiKey: String): Boolean {
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
            name = mutableMapOf(name to json["data"]!!.jsonObject["name"].finalize()),
            slug = json["data"]!!.jsonObject["slug"].finalize(),
            type = when (json["data"]!!.jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                6552 -> ProjectType.SHADER

                else -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(name to json["data"]!!.jsonObject["id"].finalize()),
            files = mutableMapOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(
            this.requestProjectBody("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug") ?: return null
        )
        if (json["data"]!!.jsonArray.isEmpty()) return null

        return Project(
            name = mutableMapOf(name to json["data"]!!.jsonArray.first().jsonObject["name"].finalize()),
            slug = json["data"]!!.jsonArray.first().jsonObject["slug"].finalize(),
            type = when (json["data"]!!.jsonArray.first().jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                6552 -> ProjectType.SHADER

                else -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(name to json["data"]!!.jsonArray.first().jsonObject["id"].finalize()),
            files = mutableMapOf(),
        )
    }

    override suspend fun requestProjectFilesFromId(
        mcVersion: String,
        loader: String,
        input: String
    ): Pair<String, List<ProjectFile>>?
    {
        val data: JsonObject = json.decodeFromString(
            this.requestProjectBody("mods/$input/files?modLoaderType=$loader") ?: return null
        )
        if (data["data"]!!.jsonArray.isEmpty()) return null

        return Pair(name, data["data"]!!.jsonArray.asSequence().filter {
            mcVersion in json.decodeFromJsonElement<List<String>>(it.jsonObject["gameVersions"]!!)
        }.map { file ->
            ProjectFile(
                fileName = file.jsonObject["fileName"].finalize(),
                mcVersion = mcVersion,
            )
        }.toList())
    }
}

