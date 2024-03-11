package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.CfModpackModel.*
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.models.MrModpackModel.File
import teksturepako.pakku.api.models.MrModpackModel.File.Env
import teksturepako.pakku.api.models.MrModpackModel.File.Hashes
import teksturepako.pakku.api.overrides.Overrides.ProjectOverride
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.REAL
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
    projectOverrides: MutableList<ProjectOverride>,
    platforms: List<Platform>,
)
{
    val outputFileName = when
    {
        configFile.getName().isBlank()    -> return onError("Could not export: 'name' is not specified")
        configFile.getVersion().isNotBlank() -> "${configFile.getName()}-${configFile.getVersion()}"
        else -> configFile.getName()
    }

    val mcVersion = lockFile.getMcVersions().firstOrNull()
        ?: return onError("Could not export: 'mc_versions' is not specified")

    if (CurseForge in platforms)
    {
        exportCurseForge(outputFileName, mcVersion, lockFile, configFile, projectOverrides).fold(
            onSuccess = { onSuccess("${CurseForge.name} modpack exported to '$it'") },
            onFailure = { onError(it.message ?: "") }
        )
    }

    if (Modrinth in platforms)
    {
        exportModrinth(outputFileName, mcVersion, lockFile, configFile, projectOverrides).fold(
            onSuccess = { onSuccess("${Modrinth.name} modpack exported to '$it'") },
            onFailure = { onError(it.message ?: "") }
        )
    }
}

suspend fun exportCurseForge(
    outputFileName: String,
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: MutableList<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()
    val create: MutableList<Pair<String, Any>> = mutableListOf()

    // File Director
    if (lockFile.getAllProjects().any { "filedirector" in it })
    {
        create += "overrides/config/mod-director/.bundle.json" to json.encodeToString(fileDirector)
    }

    val cfLoaders: List<CfModLoaderData> =
        lockFile.getLoadersWithVersions().map { dep ->
            CfModLoaderData(
                id = "${dep.first}-${dep.second}",
                primary = lockFile.getLoadersWithVersions().firstOrNull()!! == dep
            )
        }

    val cfFiles = lockFile.getAllProjects().mapNotNull { project ->
        val file = project.getFilesForPlatform(CurseForge).firstOrNull()
            ?: return@mapNotNull null.also {
                if (lockFile.getAllProjects().any { "filedirector" in it })
                {
                    project.addToFileDirectorFrom(Modrinth, fileDirector)
                }
                else if (project.redistributable)
                {
                    project.getFilesForPlatform(Modrinth).firstOrNull()?.let { file ->
                        projectOverrides += ProjectOverride(
                            project.type, file.fileName, location = REAL
                        )
                    }
                }
            }

        CfModData(
            projectID = project.id[CurseForge.serialName]!!, fileID = file.id
        )
    }

    val cfManifestData = CfModpackModel(
        CfMinecraftData(
            version = mcVersion,
            modLoaders = cfLoaders
        ),
        name = configFile.getName(),
        version = configFile.getVersion(),
        author = configFile.getAuthor(),
        files = cfFiles
    )

    // Manifest
    create += "manifest.json" to jsonEncodeDefaults.encodeToString(cfManifestData)

    // Project Overrides
    create += projectOverrides.toExportData()

    return zipFile(
        outputFileName = outputFileName,
        extension = "zip",
        overrides = configFile.getAllOverrides(),
        create = create.toTypedArray()
    )
}

suspend fun exportModrinth(
    outputFileName: String,
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectOverrides: MutableList<ProjectOverride>
): Result<String>
{
    val fileDirector = FileDirectorData()
    val create: MutableList<Pair<String, Any>> = mutableListOf()

    // File Director
    if (lockFile.getAllProjects().any { "filedirector" in it })
    {
        create += "overrides/config/mod-director/.bundle.json" to json.encodeToString(fileDirector)
    }

    val mrLoaders: List<Pair<String, String>> = lockFile.getLoadersWithVersions()
        .mapNotNull { (loaderName, loaderVersion) ->
            Modrinth.getExportLoaderName(loaderName)?.let { it to loaderVersion }
        }

    val mrFiles: Set<File> = lockFile.getAllProjects().mapNotNull { project ->
        val file = project.getFilesForPlatform(Modrinth).firstOrNull()
            ?: return@mapNotNull null.also {
                if (lockFile.getAllProjects().any { "filedirector" in it })
                {
                    project.addToFileDirectorFrom(CurseForge, fileDirector)
                }
                else if (project.redistributable)
                {
                    project.getFilesForPlatform(CurseForge).firstOrNull()?.let { file ->
                        projectOverrides += ProjectOverride(
                            project.type, file.fileName, location = REAL
                        )
                    }
                }
            }

        File(
            path = "${project.type.folderName}/${file.fileName}",
            hashes = Hashes(
                sha512 = file.hashes?.get("sha512")!!,
                sha1 = file.hashes["sha1"]!!
            ),
            env = Env(
                client = if (project.side == ProjectSide.CLIENT || project.side ==  ProjectSide.BOTH)
                    "required" else "unsupported",
                server = if (project.side == ProjectSide.SERVER || project.side ==  ProjectSide.BOTH)
                    "required" else "unsupported"
            ),
            downloads = setOf(file.url!!.replace(" ", "+")), // Replace spaces with '+' in URLs
            fileSize = file.size
        )
    }.toSet()

    val mrDependencies = mutableMapOf("minecraft" to mcVersion)
        .apply { this.putAll(mrLoaders) }

    val mrModpackModel = MrModpackModel(
        name = configFile.getName(),
        summary = configFile.getDescription(),
        versionId = configFile.getVersion(),
        files = mrFiles,
        dependencies = mrDependencies
    )

    // Index
    create += "modrinth.index.json" to jsonEncodeDefaults.encodeToString(mrModpackModel)

    // Project Overrides
    create += projectOverrides.toExportData()

    return zipFile(
        outputFileName = outputFileName,
        extension = "mrpack",
        overrides = configFile.getAllOverrides(),
        create = create.toTypedArray()
    )
}
