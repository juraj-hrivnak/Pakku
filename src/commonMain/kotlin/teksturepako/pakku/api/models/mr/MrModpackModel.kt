package teksturepako.pakku.api.models.mr

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.LockFile
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

        // CurseForge
        return if (CurseForge in platforms)
        {
            runBlocking {
                debug { println("CurseForge sub-import") }

                val projectToSlugs = projects.mapNotNull { project ->
                    project.slug[Modrinth.serialName]?.let { project to it }
                }

                val cfProjects = projectToSlugs.map { (project, slug) ->
                    async {
                        Ok(CurseForge.requestProjectFromSlug(slug)
                            .getOrElse { return@async Err(it) }
                            .apply {
                                files += CurseForge.requestFilesForProject(
                                    lockFile.getMcVersions(), lockFile.getLoaders(), this, projectType = project.type
                                ).getOrElse { return@async Err(it) }
                            }
                        )
                    }
                }.awaitAll().map { result ->
                    result.getOrElse { return@runBlocking Err(it) }
                }

                Ok(projects.combineWith(cfProjects))
            }
        }
        else Ok(projects)
    }

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