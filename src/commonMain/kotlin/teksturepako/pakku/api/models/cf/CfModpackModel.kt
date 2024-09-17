package teksturepako.pakku.api.models.cf

import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.models.ModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.debug

@Serializable
data class CfModpackModel(
    val minecraft: CfMinecraftData,
    val manifestType: String = "minecraftModpack",
    val manifestVersion: Int = 1,
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val files: MutableList<CfModData>,
    val overrides: String = "overrides"
) : ModpackModel
{
    @Serializable
    data class CfMinecraftData(
        val version: String, val modLoaders: List<CfModLoaderData>
    )

    @Serializable
    data class CfModLoaderData(
        val id: String, val primary: Boolean
    )

    @Serializable
    data class CfModData(
        val projectID: Int, val fileID: Int, val required: Boolean = true
    )

    override suspend fun toSetOfProjects(
        lockFile: LockFile, platforms: List<Platform>
    ): Set<Project>
    {
        val projects = CurseForge.requestMultipleProjects(this.files.map { it.projectID.toString() })
        val projectFiles = CurseForge.requestMultipleProjectFiles(lockFile.getMcVersions(),
            lockFile.getLoaders(),
            this.files.map { it.fileID.toString() })

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

    override suspend fun toLockFile() = LockFile(
        target = CurseForge.serialName,
        mcVersions = mutableListOf(this.minecraft.version),
        loaders = this.minecraft.modLoaders.associate {
            val (loaderName: String, loaderVersion: String) = it.id.split("-")
            loaderName to loaderVersion
        }.toMutableMap(),
        projects = mutableListOf()
    )

    companion object
    {
        const val EXTENSION = "zip"
        const val MANIFEST = "manifest.json"
    }
}