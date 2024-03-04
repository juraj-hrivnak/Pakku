package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import net.thauvin.erik.urlencoder.UrlEncoderUtil.encode
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.CfModpackModel.*
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.models.MrModpackModel.File
import teksturepako.pakku.api.models.MrModpackModel.File.Env
import teksturepako.pakku.api.overrides.Overrides.ProjectOverride
import teksturepako.pakku.api.overrides.Overrides.toExportData
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.compat.FileDirectorData
import teksturepako.pakku.compat.addToFileDirectorFrom
import teksturepako.pakku.io.zipFile

suspend fun export(
    onSuccess: suspend (message: String) -> Unit,
    onError: suspend (message: String) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: List<ProjectOverride>,
    platforms: List<Platform>,
)
{
    val mcVersion = lockFile.getMcVersions().firstOrNull()
        ?: return onError("Could not export: 'mc_versions' is not specified")

    if (CurseForge in platforms)
    {
        exportCurseForge(mcVersion, lockFile, configFile, projectOverrides).fold(
            onSuccess = { onSuccess("${CurseForge.name} modpack exported to '$it'") },
            onFailure = { onError(it.message ?: "") }
        )
    }

    if (Modrinth in platforms)
    {
        exportModrinth(mcVersion, lockFile, configFile, projectOverrides).fold(
            onSuccess = { onSuccess("${Modrinth.name} modpack exported to '$it'") },
            onFailure = { onError(it.message ?: "") }
        )
    }
}

suspend fun exportCurseForge(
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: List<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()

    val cfLoaders: List<CfModLoaderData> =
        configFile.loaders.toList().map { dep ->
            CfModLoaderData(
                id = "${dep.first}-${dep.second}",
                primary = configFile.loaders.toList().firstOrNull()!! == dep
            )
        }

    val cfFiles = lockFile.getAllProjects().mapNotNull { project ->
        val file = project.getFilesForPlatform(CurseForge).firstOrNull()
            ?: return@mapNotNull null.also { project.addToFileDirectorFrom(Modrinth, fileDirector) }

        return@mapNotNull CfModData(
            projectID = project.id[CurseForge.serialName]!!, fileID = file.id
        )
    }

    val cfManifestData = CfModpackModel(
        CfMinecraftData(
            version = mcVersion,
            modLoaders = cfLoaders
        ),
        name = lockFile.getName(),
        version = configFile.version,
        files = cfFiles
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
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: List<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()

    val mrLoaders: List<Pair<String, String>> = configFile.loaders.mapNotNull { dep ->
        Modrinth.getExportLoaderName(dep.key)?.let { it to dep.value }
    }

    val mrFiles: Set<File> = lockFile.getAllProjects().mapNotNull { project ->
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

    val mrDependencies = mutableMapOf("minecraft" to mcVersion)
        .apply { this.putAll(mrLoaders) }

    val mrModpackModel = MrModpackModel(
        name = lockFile.getName(),
        versionId = configFile.version,
        files = mrFiles,
        dependencies = mrDependencies
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
