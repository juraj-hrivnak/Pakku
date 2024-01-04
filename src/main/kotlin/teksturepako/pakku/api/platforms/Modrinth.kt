package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.MrProjectModel
import teksturepako.pakku.api.models.MrVersionModel
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
        val response = json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$id") ?: return null
        )

        return Project(
            name = mutableMapOf(serialName to response.title),
            slug = mutableMapOf(serialName to response.slug),
            type = when (response.projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type ${response.projectType} not found!") }
            },
            id = mutableMapOf(serialName to response.id),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        val response = json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$slug") ?: return null
        )

        return Project(
            name = mutableMapOf(serialName to response.title),
            slug = mutableMapOf(serialName to response.slug),
            type = when (response.projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type ${response.projectType} not found!") }
            },
            id = mutableMapOf(serialName to response.id),
            files = mutableSetOf(),
        )
    }

    // -- FILES --

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        return if (fileId == null) // Multiple files
        {
            val response = json.decodeFromString<List<MrVersionModel>>(
                this.requestProjectBody("project/$projectId/version") ?: return mutableSetOf()
            )

            response.filter { version ->
                version.gameVersions.any { it in mcVersions } && version.loaders
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.lowercase() }?.any {
                        loaders.any { loader -> loader == it } || it in listOf(
                            "minecraft", "iris", "optifine", "datapack"
                        )
                    } ?: true
            }.flatMap { version ->
                version.files.map { versionFile ->
                    MrFile(
                        fileName = versionFile.filename,
                        mcVersions = version.gameVersions.toMutableList(),
                        loaders = version.loaders.toMutableList(),
                        releaseType = version.versionType.run {
                            if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                        },
                        url = versionFile.url,
                        hashes = versionFile.hashes.let {
                            mutableMapOf(
                                "sha512" to it.sha512,
                                "sha1" to it.sha1
                            )
                        },
                        requiredDependencies = version.dependencies
                            .filter { "required" in it.dependencyType }
                            .mapNotNull { it.projectId }.toMutableSet()
                    )
                }
            }.debug {
                if (it.isEmpty()) println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
            }.toMutableSet()
        } else // One file
        {
            val response = json.decodeFromString<MrVersionModel>(
                this.requestProjectBody("version/$fileId") ?: return mutableSetOf()
            )

            response.files.map { versionFile ->
                MrFile(
                    fileName = versionFile.filename,
                    mcVersions = response.gameVersions.toMutableList(),
                    loaders = response.loaders.toMutableList(),
                    releaseType = response.versionType.run {
                        if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                    },
                    url = versionFile.url,
                    hashes = versionFile.hashes.let {
                        mutableMapOf(
                            "sha512" to it.sha512,
                            "sha1" to it.sha1
                        )
                    },
                    requiredDependencies = response.dependencies
                        .filter { "required" in it.dependencyType }
                        .mapNotNull { it.projectId }.toMutableSet()
                )
            }.toMutableSet()
        }
    }
}