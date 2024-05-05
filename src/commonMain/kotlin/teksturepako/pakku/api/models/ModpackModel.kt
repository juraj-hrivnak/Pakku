package teksturepako.pakku.api.models

import korlibs.io.async.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.debug

sealed class ModpackModel
{
    abstract suspend fun toSetOfProjects(
        lockFile: LockFile,
        platforms: List<Platform>
    ): Set<Project>

    @Serializable
    data class CfModpackModel(
        val minecraft: CfMinecraftData,
        val manifestType: String = "minecraftModpack",
        val manifestVersion: Int = 1,
        val name: String = "",
        val version: String = "",
        val author: String = "",
        val files: List<CfModData>,
        val overrides: String = "overrides"
    ) : ModpackModel()
    {
        @Serializable
        data class CfMinecraftData(
            val version: String,
            val modLoaders: List<CfModLoaderData>
        )

        @Serializable
        data class CfModLoaderData(
            val id: String,
            val primary: Boolean
        )

        @Serializable
        data class CfModData(
            val projectID: Int,
            val fileID: Int,
            val required: Boolean = true
        )

        override suspend fun toSetOfProjects(
            lockFile: LockFile,
            platforms: List<Platform>
        ): Set<Project>
        {
            val projects = CurseForge.requestMultipleProjects(this.files.map { it.projectID.toString() })
            val projectFiles = CurseForge.requestMultipleProjectFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), this.files.map { it.fileID.toString() }
            )

            projects.assignFiles(projectFiles, CurseForge)

            // Modrinth
            return if (Modrinth in platforms)
            {
                debug { println("Modrinth sub-import") }

                val slugs = projects.mapNotNull { project ->
                    project.slug[CurseForge.serialName]
                }

                val mrProjects = Modrinth.requestMultipleProjectsWithFiles(
                    lockFile.getMcVersions(), lockFile.getLoaders(), slugs, 1
                )

                projects.combineWith(mrProjects)
            }
            else projects
        }
    }

    @Serializable
    data class MrModpackModel(
        val formatVersion: Int = 1,
        val game: String = "minecraft",
        val versionId: String = "",
        val name: String = "",
        val summary: String = "",
        val files: Set<File> = setOf(),
        val dependencies: Map<String, String> = mapOf()
    ) : ModpackModel()
    {
        @Serializable
        data class File(
            val path: String,
            val hashes: Hashes,
            val env: Env? = null,
            val downloads: Set<String>,
            val fileSize: Int
        ) {
            @Serializable
            data class Hashes(
                val sha512: String,
                val sha1: String
            )

            @Serializable
            data class Env(
                val client: String = "required",
                val server: String = "required"
            )
        }

        override suspend fun toSetOfProjects(
            lockFile: LockFile,
            platforms: List<Platform>
        ): Set<Project>
        {
            val projects = Modrinth.requestMultipleProjectsWithFilesFromHashes(
                this.files.map { it.hashes.sha1 }, "sha1"
            )

            // CurseForge
            return if (CurseForge in platforms)
            {
                runBlocking {
                    debug { println("CurseForge sub-import") }

                    val slugs = projects.mapNotNull { project ->
                        project.slug[Modrinth.serialName]
                    }

                    val cfProjects = slugs.map { slug ->
                        async {
                            CurseForge.requestProjectFromSlug(slug)?.apply {
                                files += CurseForge.requestFilesForProject(
                                    lockFile.getMcVersions(), lockFile.getLoaders(), this
                                )
                            }
                        }
                    }.awaitAll().filterNotNull()

                    projects.combineWith(cfProjects).debug { println(it.map { p -> p.slug }) }
                }
            }
            else projects
        }
    }
}