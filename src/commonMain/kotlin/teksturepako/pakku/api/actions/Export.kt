package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import net.thauvin.erik.urlencoder.UrlEncoderUtil.encode
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.CfModpackModel.CfMinecraftData
import teksturepako.pakku.api.models.CfModpackModel.CfModLoaderData
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.models.MrModpackModel.File
import teksturepako.pakku.api.models.MrModpackModel.File.Env
import teksturepako.pakku.api.overrides.Overrides.ProjectOverride
import teksturepako.pakku.api.overrides.Overrides.toExportData
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.compat.FileDirectorData
import teksturepako.pakku.compat.addToFileDirectorFrom
import teksturepako.pakku.io.zipFile

suspend fun exportCurseForge(
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: List<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()

    val mcVersion = lockFile.getMcVersions().firstOrNull()!!

    val loaders: List<CfModLoaderData> =
        configFile.loaders.toList().map { dep ->
            CfModLoaderData(
                id = "${dep.first}-${dep.second}",
                primary = configFile.loaders.toList().firstOrNull()!! == dep
            )
        }

    val files = lockFile.getAllProjects().mapNotNull { project ->
        val file = project.getFilesForPlatform(CurseForge).firstOrNull()
            ?: return@mapNotNull null.also { project.addToFileDirectorFrom(Modrinth, fileDirector) }

        return@mapNotNull CfModpackModel.CfModData(
            projectID = project.id[CurseForge.serialName]!!, fileID = file.id
        )
    }

    val cfManifestData = CfModpackModel(
        CfMinecraftData(
            version = mcVersion,
            modLoaders = loaders
        ),
        name = lockFile.getName(),
        version = configFile.version,
        files = files
    )

    return zipFile(
        lockFile.getName(),
        "zip",
        configFile.getAllOverrides(),
        "manifest.json" to jsonEncodeDefaults.encodeToString(cfManifestData),
        "overrides/config/mod-director/.bundle.json" to jsonEncodeDefaults.encodeToString(fileDirector),
        *projectOverrides.toExportData()
    )
}

suspend fun exportModrinth(
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: List<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()

    val files: Set<File> = lockFile.getAllProjects().mapNotNull { project ->
        val file = project.getFilesForPlatform(Modrinth).firstOrNull()
            ?: return@mapNotNull null.also { project.addToFileDirectorFrom(CurseForge, fileDirector) }

        File(
            path = "${project.type.folderName}/${file.fileName}",
            hashes = File.Hashes(
                sha512 = file.hashes?.get("sha512")!!,
                sha1 = file.hashes["sha1"]!!
            ),
            env = Env(
                client = if (project.side == ProjectSide.CLIENT || project.side ==  ProjectSide.BOTH)
                    "required" else "unsupported",
                server = if (project.side == ProjectSide.SERVER || project.side ==  ProjectSide.BOTH)
                    "required" else "unsupported"
            ),
            downloads = setOf(encode(file.url!!)), // Encode URLs
            fileSize = file.size
        )
    }.toSet()

    val dependencies = mutableMapOf(
        "minecraft" to lockFile.getMcVersions().firstOrNull()!!,
    ).apply {
        this.putAll(
            configFile.loaders.mapNotNull { dep ->
                Modrinth.getExportLoaderName(dep.key)?.let { it to dep.value  }
            }
        )
    }

    val mrModpackModel = MrModpackModel(
        name = lockFile.getName(),
        versionId = configFile.version,
        files = files,
        dependencies = dependencies
    )

    return zipFile(
        lockFile.getName(),
        "mrpack",
        configFile.getAllOverrides(),
        "modrinth.index.json" to jsonEncodeDefaults.encodeToString(mrModpackModel),
        "overrides/config/mod-director/.bundle.json" to jsonEncodeDefaults.encodeToString(fileDirector),
        *projectOverrides.toExportData()
    )
}