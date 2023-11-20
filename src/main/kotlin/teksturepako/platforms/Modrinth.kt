package teksturepako.platforms

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import teksturepako.data.finalize
import teksturepako.data.json
import teksturepako.projects.MrFile
import teksturepako.projects.Project
import teksturepako.projects.ProjectFile
import teksturepako.projects.ProjectType

object Modrinth : Platform()
{
    override val name = "Modrinth"
    override val serialName = "modrinth"
    override val apiUrl = "https://api.modrinth.com"
    override val apiVersion = 2

    override suspend fun requestProjectFromId(id: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectBody("project/$id") ?: return null)

        return Project(
            name = mutableMapOf(this.serialName to json["title"].finalize()),
            slug = json["slug"].finalize(),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(this.serialName to json["id"].finalize()),
            files = mutableMapOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectBody("project/$slug") ?: return null)

        return Project(
            name = mutableMapOf(this.serialName to json["title"].finalize()),
            slug = json["slug"].finalize(),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(this.serialName to json["id"].finalize()),
            files = mutableMapOf(),
        )
    }

    override suspend fun requestProjectFilesFromId(
        mcVersion: String, loader: String, input: String
    ): Pair<String, List<ProjectFile>>?
    {
        val data: JsonArray = json.decodeFromString(this.requestProjectBody("project/$input/version") ?: return null)

        return Pair(name, data.filter { version ->
            mcVersion in version.jsonObject["game_versions"]!!.jsonArray.map { it.finalize() }
                    && loader in version.jsonObject["loaders"]!!.jsonArray.map { it.finalize() }
        }.flatMap { version ->
            version.jsonObject["files"]!!.jsonArray.map { file ->
                ProjectFile(
                    fileName = file.jsonObject["filename"].finalize(),
                    mcVersion = mcVersion,
                    url = file.jsonObject["url"].finalize(),
                    data = file.jsonObject["hashes"]?.let {
                        MrFile(
                            mutableMapOf(
                                "sha512" to it.jsonObject["sha512"].finalize(),
                                "sha1" to it.jsonObject["sha1"].finalize()
                            )
                        )
                    }
                )
            }
        }.toList())
    }
}