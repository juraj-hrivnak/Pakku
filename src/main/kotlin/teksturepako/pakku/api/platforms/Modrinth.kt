package teksturepako.pakku.api.platforms

import kotlinx.serialization.json.*
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

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
            files = mutableSetOf(),
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
            files = mutableSetOf(),
        )
    }

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param fileId The file ID.
     * @return A mutable list of [MrFile] objects, or null if an error occurs or no files are found.
     */
    override suspend fun requestProjectFilesFromFileId(
        mcVersion: String, loader: String, fileId: String
    ): MutableSet<ProjectFile>?
    {
        val data: JsonArray = json.decodeFromString(this.requestProjectBody("project/$fileId/version") ?: return null
            .debug {println("Error ${this.toPrettyString()}#val data = null") }
        )

        return data.filter { file ->


            mcVersion in file.jsonObject["game_versions"]!!.jsonArray.map { it.finalize() }
                    && json.decodeFromJsonElement<MutableList<String>>(file.jsonObject["loaders"]!!)
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.lowercase() }
                        ?.any {
                            loader == it || it in listOf("minecraft", "iris", "optifine", "datapack")
                        } ?: true
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
                            "sha512" to it.jsonObject["sha512"].finalize(), "sha1" to it.jsonObject["sha1"].finalize()
                        )
                    },
                    requiredDependencies = json.decodeFromJsonElement<MutableList<JsonObject>>(version.jsonObject["dependencies"]!!)
                        .filter { "required" in it["dependency_type"].finalize() }
                        .map { it["project_id"].finalize() }
                        .toMutableSet()
                )
            }
        }.debug { if (it.isEmpty()) println("Error ${this.toPrettyString()}#project file is null") }.toMutableSet()
    }

    override suspend fun requestFilesForProject(
        mcVersions: List<String>, loaders: List<String>, project: Project, numberOfFiles: Int
    ): MutableSet<ProjectFile>
    {
        val result = mutableSetOf<ProjectFile>()
        project.id[this.serialName]?.let { projectId ->
            requestProjectFilesFromFileId(mcVersions, loaders, projectId).take(numberOfFiles)
                .filterIsInstance<MrFile>()
                .also { result.addAll(it) }
        }
        return result
    }
}