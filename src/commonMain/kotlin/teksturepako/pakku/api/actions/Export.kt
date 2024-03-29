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
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.ProjectOverride
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.REAL
import teksturepako.pakku.api.overrides.Overrides.toExportData
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.cli.ui.getFlavoredProjectName
import teksturepako.pakku.compat.FileDirectorData
import teksturepako.pakku.compat.addToFileDirectorFrom
import teksturepako.pakku.io.zipModpack

suspend fun export(
    onSuccess: suspend (message: String) -> Unit,
    onError: suspend (message: String) -> Unit,
    onWarning: suspend (message: String) -> Unit,
    onInfo: suspend (message: String) -> Unit,
    path: String?,
    side: ProjectSide,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>,
)
{
    val outputFileName = when
    {
        configFile.getName().isBlank()       -> return onError("Could not export: 'name' is not specified")
        configFile.getVersion().isNotBlank() -> "${configFile.getName()}-${configFile.getVersion()}"
        else -> configFile.getName()
    }

    val mcVersion = lockFile.getMcVersions().firstOrNull()
        ?: return onError("Could not export: 'mc_versions' is not specified")

    if (CurseForge in platforms)
    {
        if (side == ProjectSide.BOTH || side == ProjectSide.CLIENT)
        {
            exportCurseForge(
                onInfo = { onInfo("(${CurseForge.name}) $it") },
                onWarning = { onWarning("(${CurseForge.name}) Warning: $it") },
                path, side, outputFileName, mcVersion, lockFile, configFile,
            ).fold(
                onSuccess = { onSuccess("(${CurseForge.name}) modpack exported to '$it'") },
                onFailure = { onError("(${CurseForge.name}) Error: ${it.message}") }
            )
        }

        if (side == ProjectSide.BOTH || side == ProjectSide.SERVER)
        {
            exportCurseForge(
                onInfo = { onInfo("(${CurseForge.name} Server Pack) $it") },
                onWarning = { onWarning("(${CurseForge.name} Server Pack) Warning: $it") },
                path, ProjectSide.SERVER, "$outputFileName-Server-Pack", mcVersion, lockFile, configFile,
            ).fold(
                onSuccess = { onSuccess("(${CurseForge.name} Server Pack) modpack exported to '$it'") },
                onFailure = { onError("(${CurseForge.name} Server Pack) Error: ${it.message}") }
            )
        }
    }

    onInfo("")

    if (Modrinth in platforms)
    {
        exportModrinth(
            onInfo = { onInfo("(${Modrinth.name}) $it") },
            onWarning = { onWarning("(${Modrinth.name}) Warning: $it") },
            path, outputFileName, mcVersion, lockFile, configFile,
        ).fold(
            onSuccess = { onSuccess("(${Modrinth.name}) modpack exported to '$it'") },
            onFailure = { onError("(${Modrinth.name}) Error: ${it.message}") }
        )
    }
}

suspend fun onProjectMissing(
    onInfo: suspend (message: String) -> Unit,
    onWarning: suspend (message: String) -> Unit,
    project: Project,
    platform: Platform,
    lockFile: LockFile,
    fileDirector: FileDirectorData,
    projectOverrides: MutableList<ProjectOverride>,
)
{
    if (!project.redistributable)
    {
        return onWarning(
            "${project.getFlavoredProjectName()} could not be exported as 'project override'" +
                    " because it is not redistributable"
        )
    }

    if (lockFile.getAllProjects().any { "filedirector" in it })
    {
        project.addToFileDirectorFrom(platform, fileDirector)
        onInfo("${project.getFlavoredProjectName()} exported to file director config")
    }
    else
    {
        project.getFilesForPlatform(platform).firstOrNull()?.let { file ->
            projectOverrides += ProjectOverride(
                project.type, file.fileName, location = REAL
            )
            onInfo("${project.getFlavoredProjectName()} exported as 'project override'")
        }
    }
}

suspend fun exportCurseForge(
    onInfo: suspend (message: String) -> Unit,
    onWarning: suspend (message: String) -> Unit,
    path: String?,
    side: ProjectSide,
    outputFileName: String,
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile
): Result<String>
{
    val projects = lockFile.getAllProjects().filter { project ->
        when
        {
            project.side == null -> true

            side == ProjectSide.BOTH -> true
            side == ProjectSide.CLIENT -> project.side == ProjectSide.CLIENT
            side == ProjectSide.SERVER -> project.side == ProjectSide.SERVER || project.side == ProjectSide.BOTH

            else                     -> false
        }
    }

    val projectOverrides = Overrides.getProjectOverrides().toMutableList()

    val fileDirector = FileDirectorData()
    val create: MutableList<Pair<String, Any>> = mutableListOf()

    // File Director
    if (projects.any { "filedirector" in it })
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

    val cfFiles = projects.mapNotNull { project ->
        val file = project.getFilesForPlatform(CurseForge).firstOrNull()
            ?: return@mapNotNull null.also { _ ->
                onProjectMissing(
                    onInfo = { onInfo(it) },
                    onWarning = { onWarning(it) },
                    project, Modrinth, lockFile, fileDirector, projectOverrides
                )
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
    onInfo("${cfManifestData.files.size} projects exported to 'manifest.json'")

    // Project Overrides
    create += projectOverrides.toExportData().map { result ->
        result.getOrElse { return Result.failure(it) }
    }
    onInfo("${projectOverrides.size} project overrides exported to the 'overrides' folder")

    if (side == ProjectSide.SERVER)
    {
        return zipModpack(
            path = path,
            outputFileName = outputFileName,
            extension = "zip",
            overrides = configFile.getAllOverrides()
                    + configFile.getAllServerOverrides(),
            serverOverrides = listOf(),
            clientOverrides = listOf(),
            create = create.toTypedArray()
        )
    }
    else
    {
        return zipModpack(
            path = path,
            outputFileName = outputFileName,
            extension = "zip",
            overrides = configFile.getAllOverrides()
                    + configFile.getAllServerOverrides()
                    + configFile.getAllClientOverrides(),
            serverOverrides = listOf(),
            clientOverrides = listOf(),
            create = create.toTypedArray()
        )
    }
}

suspend fun exportModrinth(
    onInfo: suspend (message: String) -> Unit,
    onWarning: suspend (message: String) -> Unit,
    path: String?,
    outputFileName: String,
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
): Result<String>
{
    val projects = lockFile.getAllProjects()

    val projectOverrides = Overrides.getProjectOverrides().toMutableList()

    val fileDirector = FileDirectorData()
    val create: MutableList<Pair<String, Any>> = mutableListOf()

    // File Director
    if (projects.any { "filedirector" in it })
    {
        create += "overrides/config/mod-director/.bundle.json" to json.encodeToString(fileDirector)
    }

    val mrLoaders: List<Pair<String, String>> = lockFile.getLoadersWithVersions()
        .mapNotNull { (loaderName, loaderVersion) ->
            Modrinth.getExportLoaderName(loaderName)?.let { it to loaderVersion }
        }

    val mrFiles: Set<File> = projects.mapNotNull { project ->
        val file = project.getFilesForPlatform(Modrinth).firstOrNull()
            ?: return@mapNotNull null.also {
                onProjectMissing(
                    onInfo = { onInfo(it) },
                    onWarning = { onWarning(it) },
                    project, CurseForge, lockFile, fileDirector, projectOverrides
                )
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
    onInfo("${mrModpackModel.files.size} projects exported to 'modrinth.index.json'")

    // Project Overrides
    create += projectOverrides.toExportData().map { result ->
        result.getOrElse { return Result.failure(it) }
    }
    onInfo("${projectOverrides.size} project overrides exported to the 'overrides' folder")

    return zipModpack(
        path = path,
        outputFileName = outputFileName,
        extension = "mrpack",
        overrides = configFile.getAllOverrides(),
        serverOverrides = configFile.getAllServerOverrides(),
        clientOverrides = configFile.getAllClientOverrides(),
        create = create.toTypedArray()
    )
}
