package teksturepako.pakku.api.models.mr

import com.github.michaelbull.result.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.http.RequestError
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.debug

@Serializable
data class MrModpackModel(
    val formatVersion: Int = 1,
    val game: String = "minecraft",
    val versionId: String = "",
    val name: String = "",
    val summary: String = "",
    val files: MutableSet<MrFile> = mutableSetOf(),
    val dependencies: Map<String, String> = mapOf()
) : ModpackModel
{
    @Serializable
    data class MrFile(
        val path: String, val hashes: Hashes, val env: Env? = null, val downloads: Set<String>, val fileSize: Int
    )
    {
        @Serializable
        data class Hashes(
            val sha512: String, val sha1: String
        )

        @Serializable
        data class Env(
            val client: String = "required", val server: String = "required"
        )
    }

    override suspend fun toSetOfProjects(
        lockFile: LockFile, platforms: List<Platform>
    ): Result<Set<Project>, ActionError>
    {

        val projects = Modrinth.requestMultipleProjectsWithFilesFromHashes(
            this.files.map { it.hashes.sha1 }, "sha1"
        ).getOrElse { return Err(it) }

        if (CurseForge !in platforms)
        {
            return Ok(projects)
        }

        debug { println("CurseForge sub-import") }

        val projectToSlugs = projects.mapNotNull { project ->
            project.slug[Modrinth.serialName]?.let { project to it }
        }

        val cfProjects = coroutineScope {
            projectToSlugs.map { (project, slug) ->
                async {
                    CurseForge.requestProjectFromSlug(slug).fold(
                        success = { cfProject ->
                            CurseForge.requestFilesForProject(
                                lockFile.getMcVersions(), lockFile.getLoaders(), cfProject, projectType = project.type
                            ).fold(
                                success = { files ->
                                    cfProject.apply {
                                        this.files += files
                                    }

                                    Ok(cfProject)
                                },

                                failure = { error ->
                                    Err(error)
                                })
                        },

                        failure = { error ->
                            if (error.isNotFound())
                            {
                                println(
                                    "No ${project.type} {mr=**$slug**} found on CurseForge."
                                )

                                Ok(null)
                            }
                            else
                            {
                                Err(error)
                            }
                        })
                }
            }.awaitAll()
        }

        val foundCfProjects = cfProjects.mapNotNull { result ->
            result.getOrElse { return Err(it) }
        }

        return Ok(projects.combineWith(foundCfProjects))
    }

    private fun ActionError.isNotFound(): Boolean =
        this is ProjNotFound || (this is RequestError && response.status == HttpStatusCode.NotFound)

    override suspend fun toLockFile() = LockFile(
        target = Modrinth.serialName,
        mcVersions = mutableListOf(this.dependencies["minecraft"] ?: ""),
        loaders = this.dependencies.filterNot { it.key == "minecraft" }.toMutableMap(),
        projects = mutableListOf()
    )

    companion object
    {
        const val EXTENSION = "mrpack"
        const val MANIFEST = "modrinth.index.json"
    }
}