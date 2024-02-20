package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.CfModpackModel.*
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.PROJECT_OVERRIDES_FOLDER
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.io.zip

class Export : CliktCommand("Export modpack")
{
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = pakkuLock.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        if (CurseForge in platforms)
        {
            // -- CURSEFORGE --

            val cfManifestData = CfModpackModel(CfMinecraftData(version = pakkuLock.getMcVersions().firstOrNull()!!,
                modLoaders = pakkuLock.getLoaders().map { loader ->
                    CfModLoaderData(
                        id = loader, primary = pakkuLock.getLoaders().firstOrNull()!! == loader
                    )
                }),
                name = pakkuLock.getPackName(),
                files = pakkuLock.getAllProjects().mapNotNull { project ->
                    val file = project.getFilesForPlatform(CurseForge).firstOrNull() ?: return@mapNotNull null
                    CfModData(
                        projectID = project.id[CurseForge.serialName]!!, fileID = file.id
                    )
                }
            )

            zip(
                pakkuLock.getPackName(),
                "zip",
                pakkuLock.getAllOverrides(),
                Overrides.getProjectOverrides().toTypedArray(),
                "/manifest.json" to jsonEncodeDefaults.encodeToString(cfManifestData),
                *Overrides.getProjectOverrides().map {
                    "/overrides/${it.type.folderName}/${it.fileName}" to localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${it.type.folderName}/${it.fileName}"].readBytes()
                }.toTypedArray()
            ).fold(
                onSuccess = { terminal.success("${CurseForge.name} modpack exported to '$it'") },
                onFailure = { terminal.danger(it.message) }
            )

        }
        else if (Modrinth in platforms)
        {
            // -- MODRINTH --

            val mrModpackModel = MrModpackModel(
                name = pakkuLock.getPackName(),
                files = pakkuLock.getAllProjects().mapNotNull { project ->
                    val file = project.getFilesForPlatform(Modrinth).firstOrNull() ?: return@mapNotNull null
                    MrModpackModel.File(
                        path = "${project.type.folderName}/${file.fileName}",
                        hashes = MrModpackModel.File.Hashes(
                            sha512 = file.hashes?.get("sha512")!!,
                            sha1 = file.hashes["sha1"]!!
                        ),
                        downloads = setOf(file.url!!),
                        fileSize = file.size
                    )
                }.toSet()
            )

            zip(pakkuLock.getPackName(),
                "mrpack",
                pakkuLock.getAllOverrides(),
                Overrides.getProjectOverrides().toTypedArray(),
                "/modrinth.index.json" to jsonEncodeDefaults.encodeToString(mrModpackModel),
                *Overrides.getProjectOverrides().map {
                    "/overrides/${it.type.folderName}/${it.fileName}" to localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${it.type.folderName}/${it.fileName}"].readBytes()
                }.toTypedArray()
            ).fold(
                onSuccess = { terminal.success("${Modrinth.name} modpack exported to '$it'") },
                onFailure = { terminal.danger(it.message) }
            )
        }

        echo()
    }
}