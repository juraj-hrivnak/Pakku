package teksturepako.pakku.api.platforms

import kotlinx.serialization.json.*
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

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
            name = mutableMapOf(serialName to json["title"].finalize()),
            slug = mutableMapOf(serialName to json["slug"].finalize()),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(serialName to json["id"].finalize()),
            files = mutableListOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val json: JsonObject = json.decodeFromString(this.requestProjectBody("project/$slug") ?: return null)

        return Project(
            name = mutableMapOf(serialName to json["title"].finalize()),
            slug = mutableMapOf(serialName to json["slug"].finalize()),
            type = when (json["project_type"].finalize())
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> throw Exception("Project type not found!")
            },
            id = mutableMapOf(serialName to json["id"].finalize()),
            files = mutableListOf(),
        )
    }

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param input The file ID.
     * @return A mutable list of [MrFile] objects, or null if an error occurs or no files are found.
     */
    override suspend fun requestProjectFilesFromId(
        mcVersion: String, loader: String, input: String
    ): MutableList<ProjectFile>?
    {
        val data: JsonArray = json.decodeFromString(this.requestProjectBody("project/$input/version") ?: return null)

        return data.filter { file ->
            mcVersion in file.jsonObject["game_versions"]!!.jsonArray.map { it.finalize() } && loader in file.jsonObject["loaders"]!!.jsonArray.map { it.finalize() }
        }.flatMap { version ->
            version.jsonObject["files"]!!.jsonArray.map { file ->
                MrFile(fileName = file.jsonObject["filename"].finalize(),
                    mcVersions = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["game_versions"]!!),
                    loaders = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["loaders"]!!),
                    releaseType = file.jsonObject["version_type"].finalize().run {
                        if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                    },
                    url = file.jsonObject["url"].finalize(),
                    hashes = file.jsonObject["hashes"]?.let {
                        mutableMapOf(
                            "sha512" to it.jsonObject["sha512"].finalize(), "sha1" to it.jsonObject["sha1"].finalize()
                        )
                    })
            }
        }.toMutableList()
    }
}