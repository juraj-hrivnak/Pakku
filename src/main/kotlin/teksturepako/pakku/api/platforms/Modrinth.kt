package teksturepako.pakku.api.platforms

import kotlinx.serialization.json.*
import teksturepako.pakku.api.data.finalize
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug

object Modrinth : Platform()
{
    override val name = "Modrinth"
    override val serialName = "modrinth"
    override val apiUrl = "https://api.modrinth.com"
    override val apiVersion = 2


    // -- PROJECT --

    override suspend fun requestProject(input: String): Project? = when
    {
        input.matches("[0-9]{6}".toRegex()) -> null
        input.matches("\b[0-9a-zA-Z]{8}\b".toRegex()) -> requestProjectFromId(input)
        else -> requestProjectFromSlug(input)
    }

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

    // -- FILES --

    /**
     * Requests project files based on Minecraft version, loader, and a file ID.
     *
     * @param mcVersion The Minecraft version.
     * @param loader The mod loader type.
     * @param projectId The project ID.
     * @return A mutable list of [MrFile] objects, or null if an error occurs or no files are found.
     */
    override suspend fun requestProjectFilesFromId(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        val requestUrl = if (fileId == null) "project/$projectId/version" else "version/$fileId"

        val data: JsonArray = json.decodeFromString(this.requestProjectBody(requestUrl) ?: return mutableSetOf())

        return if (fileId == null) {
            data.filter { file ->
                // mcVersions
                file.jsonObject["game_versions"]!!.jsonArray.map { it.finalize() }.any { it in mcVersions }
                        // Loaders
                        && json.decodeFromJsonElement<MutableList<String>>(file.jsonObject["loaders"]!!)
                            .takeIf { it.isNotEmpty() }
                            ?.map { it.lowercase() }
                            ?.any {
                                loaders.any { loader -> loader == it }
                                        // Loaders valid by default
                                        || it in listOf("minecraft", "iris", "optifine", "datapack")
                            } ?: true
            }.flatMap { version ->
                version.jsonObject["files"]!!.jsonArray.map { file ->
                    MrFile(
                        fileName = file.jsonObject["filename"].finalize(),
                        mcVersions = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["game_versions"]!!),
                        loaders = json.decodeFromJsonElement<MutableList<String>>(version.jsonObject["loaders"]!!),
                        releaseType = version.jsonObject["version_type"].finalize().run {
                            if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                        },
                        url = file.jsonObject["url"].finalize(),
                        hashes = file.jsonObject["hashes"]?.let {
                            mutableMapOf(
                                "sha512" to it.jsonObject["sha512"].finalize(),
                                "sha1" to it.jsonObject["sha1"].finalize()
                            )
                        },
                        requiredDependencies = json.decodeFromJsonElement<MutableList<JsonObject>>(version.jsonObject["dependencies"]!!)
                            .filter { "required" in it["dependency_type"].finalize() }
                            .map { it["project_id"].finalize() }.toMutableSet()
                    )
                }
            }.debug {
                if (it.isEmpty()) println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
            }.toMutableSet()
        } else
        {
            mutableSetOf(MrFile(
                fileName = data.jsonObject["filename"].finalize(),
                mcVersions = json.decodeFromJsonElement<MutableList<String>>(data.jsonObject["game_versions"]!!),
                loaders = json.decodeFromJsonElement<MutableList<String>>(data.jsonObject["loaders"]!!),
                releaseType = data.jsonObject["version_type"].finalize().run {
                    if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                },
                url = data.jsonObject["url"].finalize(),
                hashes = data.jsonObject["hashes"]?.let {
                    mutableMapOf(
                        "sha512" to it.jsonObject["sha512"].finalize(),
                        "sha1" to it.jsonObject["sha1"].finalize()
                    )
                },
                requiredDependencies = json.decodeFromJsonElement<MutableList<JsonObject>>(data.jsonObject["dependencies"]!!)
                    .filter { "required" in it["dependency_type"].finalize() }
                    .map { it["project_id"].finalize() }.toMutableSet()
            ))
        }
    }
}