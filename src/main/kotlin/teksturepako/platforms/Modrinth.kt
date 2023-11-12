package teksturepako.platforms

import kotlinx.serialization.json.JsonObject
import teksturepako.debug
import teksturepako.json.json
import teksturepako.platforms.projects.Project
import teksturepako.platforms.projects.ProjectType

object Modrinth : Platform()
{
    override val apiUrl = "https://api.modrinth.com"
    override val apiVersion = 2

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectString("project/$slug") ?: return null)
        debug { println(json["project_type"].toString().replace("\"", "") + " ") }
        return Project(
            name = json["title"].toString(),
            slug = json["slug"].toString(),
            type = when (json["project_type"].toString().replace("\"", ""))
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"   -> ProjectType.SHADER_PACK

                else           -> throw Exception("Project type not found!")
            },
        )
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectString("project/$id") ?: return null)
        debug { println(json["project_type"].toString().replace("\"", "") + " ") }
        return Project(
            name = json["title"].toString(),
            slug = json["slug"].toString(),
            type = when (json["project_type"].toString().replace("\"", ""))
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"   -> ProjectType.SHADER_PACK

                else           -> throw Exception("Project type not found!")
            },
        )
    }
}