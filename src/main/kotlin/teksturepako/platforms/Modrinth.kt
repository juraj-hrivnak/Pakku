package teksturepako.platforms

import kotlinx.serialization.json.*
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
            slug = mutableMapOf(this.serialName to json["slug"].finalize()),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(this.serialName to json["id"].finalize()),
            files = mutableListOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectBody("project/$slug") ?: return null)

        return Project(
            name = mutableMapOf(this.serialName to json["title"].finalize()),
            slug = mutableMapOf(this.serialName to json["slug"].finalize()),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(this.serialName to json["id"].finalize()),
            files = mutableListOf(),
        )
    }

    override suspend fun requestProjectFilesFromId(
        mcVersion: String, loader: String, input: String
    ): MutableList<ProjectFile>?
    {
        val data: JsonArray = json.decodeFromString(this.requestProjectBody("project/$input/version") ?: return null)

        return data.filter { file ->
            mcVersion in file.jsonObject["game_versions"]!!.jsonArray.map { it.finalize() }
                    && loader in file.jsonObject["loaders"]!!.jsonArray.map { it.finalize() }
        }.flatMap { version ->
            version.jsonObject["files"]!!.jsonArray.map { file ->
                MrFile(
                    fileName = file.jsonObject["filename"].finalize(),
                    mcVersions = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["game_versions"]!!),
                    loaders = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["loaders"]!!),
                    releaseType = file.jsonObject["version_type"].finalize().run {
                        if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                    },
                    url = file.jsonObject["url"].finalize(),
                    hashes = file.jsonObject["hashes"]?.let {
                        mutableMapOf(
                            "sha512" to it.jsonObject["sha512"].finalize(),
                            "sha1" to it.jsonObject["sha1"].finalize()
                        )
                    }
                )
            }
        }.toMutableList()
    }
}