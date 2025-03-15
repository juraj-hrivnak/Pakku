package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.ExportRuleScope
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.*
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel.MrFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readPathBytesOrNull

fun ExportRuleScope.mrModpackRule(): ExportRule
{
    val modpackModel = lockFile.getFirstMcVersion()?.run {
        createMrModpackModel(this, lockFile, configFile)
    }

    return ExportRule {
        when (it)
        {
            is ExportingProject         ->
            {
                val projectFile = it.project.getFilesForProviders(Modrinth, GitHub).firstOrNull()
                    ?: return@ExportRule it.setMissing()

                it.addToMrModpackModel(projectFile, modpackModel ?: return@ExportRule it.error(RequiresMcVersion))
            }
            is ExportingOverride       -> it.export()
            is ExportingManualOverride -> it.export()
            is Finished                ->
            {
                it.createJsonFile(modpackModel, MrModpackModel.MANIFEST, format = jsonEncodeDefaults)
            }
            else                        -> it.ignore()
        }
    }
}

fun mrMissingProjectsRule() = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.exportAsOverride(excludedProviders = setOf(Modrinth, GitHub)) { bytesCallback, fileName, overridesDir ->
                it.createFile(bytesCallback, overridesDir, it.project.getPathStringWithSubpath(it.configFile), fileName)
            }
        }
        else -> it.ignore()
    }
}

fun ExportingProject.addToMrModpackModel(projectFile: ProjectFile, modpackModel: MrModpackModel) =
    ruleResult("addToMrModpackModel ${project.type} ${project.slug}", Packaging.Action {
        projectFile.toMrFile(lockFile, configFile)?.let { mrFile ->
            modpackModel.files.add(mrFile)
        }
        null // Return no error
    })

fun getMrLoaderName(loader: String) = when (loader)
{
    "fabric" -> "fabric-loader"
    "quilt"  -> "quilt-loader"
    else     -> loader
}

fun createMrModpackModel(
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
): MrModpackModel
{
    val mrDependencies = mapOf("minecraft" to mcVersion) + lockFile.getLoadersWithVersions()
        .associate { (loaderName, loaderVersion) ->
            getMrLoaderName(loaderName) to loaderVersion
        }

    return MrModpackModel(
        name = configFile.getName(),
        summary = configFile.getDescription(),
        versionId = configFile.getVersion(),
        dependencies = mrDependencies
    )
}

suspend fun ProjectFile.toMrFile(lockFile: LockFile, configFile: ConfigFile): MrFile?
{
    if (this.type !in listOf(Modrinth.serialName, GitHub.serialName)) return null

    val parentProject = this.getParentProject(lockFile) ?: return null

    val relativePathString = this.getRelativePathString(parentProject, configFile)
    val path = this.getPath(parentProject, configFile)

    val serverSide = if (parentProject.side in listOf(ProjectSide.SERVER, ProjectSide.BOTH) || parentProject.side == null)
    {
        "required"
    }
    else "unsupported"

    return MrFile(
        path = relativePathString,
        hashes = MrFile.Hashes(
            sha512 = this.hashes?.get("sha512")
                ?: readPathBytesOrNull(path)?.let { bytes ->
                    createHash("sha512", bytes)
                } ?: return null,
            sha1 = this.hashes?.get("sha1")
                ?: readPathBytesOrNull(path)?.let { bytes ->
                    createHash("sha1", bytes)
                } ?: return null,
        ),
        env = MrFile.Env(
            client = "required",
            server = serverSide,
        ),
        // Replace ' ' in URL with '+'
        downloads = setOf(this.url!!.replace(" ", "+")),
        fileSize = this.size
    )
}