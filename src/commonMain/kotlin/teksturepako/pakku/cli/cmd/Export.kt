package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.CfModpackModel.*
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.toExportData
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.compat.FileDirectorData
import teksturepako.pakku.io.zipFile


class Export : CliktCommand("Export modpack")
{
    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectOverrides = Overrides.getProjectOverrides().toExportData()

        // -- CURSEFORGE --

        if (CurseForge in platforms)
        {
            val fileDirector = FileDirectorData()

            val cfManifestData = CfModpackModel(
                CfMinecraftData(
                    version = lockFile.getMcVersions().firstOrNull()!!,
                    modLoaders = configFile.loaders.toList().map { dep ->
                        CfModLoaderData(
                            id = "${dep.first}-${dep.second}",
                            primary = configFile.loaders.toList().firstOrNull()!! == dep
                        )
                    }
                ),
                name = lockFile.getName(),
                version = configFile.version,
                files = lockFile.getAllProjects().mapNotNull { project ->
                    val file = project.getFilesForPlatform(CurseForge).firstOrNull()
                        ?: return@mapNotNull null.also {
                            project.getFilesForPlatform(Modrinth).firstOrNull()?.let {
                                if (it.url != null) fileDirector.urlBundle.add(FileDirectorData.UrlEntry(
                                    url = it.url!!.replace(" ", "+"),
                                    folder = project.type.folderName
                                ))
                            }
                        }
                    CfModData(
                        projectID = project.id[CurseForge.serialName]!!, fileID = file.id
                    )
                }
            )

            zipFile(
                lockFile.getName(),
                "zip",
                configFile.getAllOverrides(),
                "manifest.json" to jsonEncodeDefaults.encodeToString(cfManifestData),
                "overrides/config/mod-director/.bundle.json" to jsonEncodeDefaults.encodeToString(fileDirector),
                *projectOverrides
            ).fold(
                onSuccess = { terminal.success("${CurseForge.name} modpack exported to '$it'") },
                onFailure = { terminal.danger(it.message) }
            )
        }

        // -- MODRINTH --

        if (Modrinth in platforms)
        {
            val fileDirector = FileDirectorData()

            val mrModpackModel = MrModpackModel(
                name = lockFile.getName(),
                versionId = configFile.version,
                files = lockFile.getAllProjects().mapNotNull { project ->
                    val file = project.getFilesForPlatform(Modrinth).firstOrNull()
                        ?: return@mapNotNull null.also {
                            project.getFilesForPlatform(CurseForge).firstOrNull()?.let {
                                if (it.url != null) fileDirector.curseBundle.add(FileDirectorData.CurseEntry(
                                    addonId = project.id[CurseForge.serialName]!!,
                                    fileId = it.id,
                                    folder = project.type.folderName
                                ))
                            }
                        }

                    MrModpackModel.File(
                        path = "${project.type.folderName}/${file.fileName}",
                        hashes = MrModpackModel.File.Hashes(
                            sha512 = file.hashes?.get("sha512")!!,
                            sha1 = file.hashes["sha1"]!!
                        ),
                        env = MrModpackModel.File.Env(
                            client = if (project.side == ProjectSide.CLIENT || project.side ==  ProjectSide.BOTH) "required" else "unsupported",
                            server = if (project.side == ProjectSide.SERVER || project.side ==  ProjectSide.BOTH) "required" else "unsupported"
                        ),
                        downloads = setOf(file.url!!),
                        fileSize = file.size
                    )
                }.toSet(),
                dependencies = mutableMapOf(
                    "minecraft" to lockFile.getMcVersions().firstOrNull()!!,
                ).apply { this.putAll(
                    configFile.loaders.mapNotNull { dep ->
                        Modrinth.getExportLoaderName(dep.key)?.let { it to dep.value  }
                    }
                ) }
            )

            zipFile(
                lockFile.getName(),
                "mrpack",
                configFile.getAllOverrides(),
                "modrinth.index.json" to jsonEncodeDefaults.encodeToString(mrModpackModel),
                "overrides/config/mod-director/.bundle.json" to jsonEncodeDefaults.encodeToString(fileDirector),
                *projectOverrides
            ).fold(
                onSuccess = { terminal.success("${Modrinth.name} modpack exported to '$it'") },
                onFailure = { terminal.danger(it.message) }
            )
        }

        echo()
    }
}