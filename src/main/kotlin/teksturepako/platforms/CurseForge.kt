package teksturepako.platforms

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import teksturepako.debug
import teksturepako.json.json
import teksturepako.platforms.projects.Project
import teksturepako.platforms.projects.ProjectType

object CurseForge : Platform()
{
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

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(
            this.requestProjectString("mods/search?gameId=432&pageSize=1&sortField=6&sortOrder=desc&slug=$slug") ?: return null
        )
        if (json["data"]!!.jsonArray.isEmpty()) return null
        debug { println(json["data"]!!.jsonArray.first().jsonObject["classId"]!!.toString() + " ") }
        return Project(
            name = json["data"]!!.jsonArray.first().jsonObject["name"].toString(),
            slug = json["data"]!!.jsonArray.first().jsonObject["slug"].toString(),
            type = when (json["data"]!!.jsonArray.first().jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                6552 -> ProjectType.SHADER_PACK

                else -> throw Exception("Project type not found!")
            }
        )
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectString("mods/$id") ?: return null)
        debug { println(json["data"]!!.jsonObject["classId"]!!.toString() + " ") }
        return Project(
            name = json["data"]!!.jsonObject["name"].toString(),
            slug = json["data"]!!.jsonObject["slug"].toString(),
            type = when (json["data"]!!.jsonObject["classId"]!!.toString().toInt())
            {
                6    -> ProjectType.MOD
                12   -> ProjectType.RESOURCE_PACK
                6552 -> ProjectType.SHADER_PACK

                else -> throw Exception("Project type not found!")
            }
        )
    }
}

