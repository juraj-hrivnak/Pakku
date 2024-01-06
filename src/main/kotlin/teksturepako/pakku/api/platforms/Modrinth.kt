package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.MrProjectModel
import teksturepako.pakku.api.models.MrVersionModel
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debugIfEmpty

object Modrinth : Platform(
    name = "Modrinth",
    serialName = "modrinth",
    apiUrl = "https://api.modrinth.com",
    apiVersion = 2
)
{
    // -- PROJECT --

    override suspend fun requestProject(input: String): Project? = when
    {
        input.matches("[0-9]{6}".toRegex()) -> null
        input.matches("\b[0-9a-zA-Z]{8}\b".toRegex()) -> requestProjectFromId(input)
        else -> requestProjectFromSlug(input)
    }

    private fun MrProjectModel.toProject(): Project?
    {
        return Project(
            name = mutableMapOf(serialName to this.title),
            slug = mutableMapOf(serialName to this.slug),
            type = when (this.projectType)
            {
                "mod"          -> ProjectType.MOD
                "resourcepack" -> ProjectType.RESOURCE_PACK
                "shader"       -> ProjectType.SHADER

                else           -> return null.also { println("Project type ${this.projectType} not found!") }
            },
            id = mutableMapOf(serialName to this.id),
            files = mutableSetOf(),
        )
    }

    override suspend fun requestProjectFromId(id: String): Project?
    {
        return json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$id") ?: return null
        ).toProject()
    }

    override suspend fun requestProjectFromSlug(slug: String): Project?
    {
        return json.decodeFromString<MrProjectModel>(
            this.requestProjectBody("project/$slug") ?: return null
        ).toProject()
    }

    override suspend fun requestMultipleProjects(ids: List<String>): MutableSet<Project>
    {
        return json.decodeFromString<List<MrProjectModel>>(
            this.requestProjectBody("projects?ids=${
                ids.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }") ?: return mutableSetOf()
        ).mapNotNull { it.toProject() }.toMutableSet()
    }

    // -- FILES --

    private fun List<MrVersionModel>.filterFileModels(
        mcVersions: List<String>, loaders: List<String>
    ): List<MrVersionModel> = this
        .filter { version ->
            version.gameVersions.any { it in mcVersions } && version.loaders
                .takeIf { it.isNotEmpty() }
                ?.map { it.lowercase() }?.any {
                    loaders.any { loader -> loader == it } || it in validLoaders // Check default valid loaders
                } ?: true
        }

    private fun MrVersionModel.toProjectFiles(): List<ProjectFile>
    {
        return this.files.map { versionFile ->
            MrFile(
                fileName = versionFile.filename,
                mcVersions = this.gameVersions.toMutableList(),
                loaders = this.loaders.toMutableList(),
                releaseType = this.versionType.run {
                    if (isNullOrBlank() || this == "null") "release" else this // TODO: Maybe not good?
                },
                url = versionFile.url,
                id = this.id,
                parentId = this.projectId,
                hashes = versionFile.hashes.let {
                    mutableMapOf(
                        "sha512" to it.sha512,
                        "sha1" to it.sha1
                    )
                },
                requiredDependencies = this.dependencies
                    .filter { "required" in it.dependencyType }
                    .mapNotNull { it.projectId }.toMutableSet()
            )
        }
    }

    override suspend fun requestProjectFiles(
        mcVersions: List<String>, loaders: List<String>, projectId: String, fileId: String?
    ): MutableSet<ProjectFile>
    {
        return if (fileId == null)
        {
            // Multiple files
            json.decodeFromString<List<MrVersionModel>>(
                this.requestProjectBody("project/$projectId/version") ?: return mutableSetOf()
            )
                .filterFileModels(mcVersions, loaders)
                .flatMap { version -> version.toProjectFiles() }
                .debugIfEmpty {
                    println("${this.javaClass.simpleName}#requestProjectFilesFromId: file is null")
                }.toMutableSet()
        } else
        {
            // One file
            json.decodeFromString<MrVersionModel>(
                this.requestProjectBody("version/$fileId") ?: return mutableSetOf()
            )
                .toProjectFiles()
                .toMutableSet()
        }
    }

    override suspend fun requestMultipleProjectFiles(
        mcVersions: List<String>, loaders: List<String>, ids: List<String>
    ): MutableSet<ProjectFile>
    {
        return json.decodeFromString<List<MrVersionModel>>(
            this.requestProjectBody("versions?ids=${
                ids.map { "%22$it%22" }.toString()
                    .replace("[", "%5B")
                    .replace("]","%5D")
            }") ?: return mutableSetOf()
        )
            .filterFileModels(mcVersions, loaders)
            .flatMap { version -> version.toProjectFiles() }
            .toMutableSet()
    }
}