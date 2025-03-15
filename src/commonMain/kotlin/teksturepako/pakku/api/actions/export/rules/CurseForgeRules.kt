package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ErrorSeverity
import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.ExportRuleScope
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.*
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.models.cf.CfModpackModel.*
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

data object RequiresMcVersion : ActionError()
{
    override val rawMessage = "Modpack requires a minimum of one version of Minecraft."
    override val severity = ErrorSeverity.FATAL
}

fun ExportRuleScope.cfModpackRule(): ExportRule
{
    val modpackModel = lockFile.getFirstMcVersion()?.let { mcVersion ->
        createCfModpackModel(mcVersion, lockFile, configFile)
    }

    return ExportRule {
        when (it)
        {
            is ExportingProject         ->
            {
                val projectFile = it.project.getFilesForPlatform(CurseForge).firstOrNull()
                    ?: return@ExportRule it.setMissing()

                it.addToCfModpackModel(projectFile, modpackModel
                    ?: return@ExportRule it.error(RequiresMcVersion))
            }
            is ExportingOverride       -> it.export(
                overridesDir = OverrideType.OVERRIDE.folderName,
                allowedTypes = setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
            )
            is ExportingManualOverride -> it.export(
                overridesDir = OverrideType.OVERRIDE.folderName,
                allowedTypes = setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
            )
            is Finished                ->
            {
                it.createJsonFile(modpackModel, CfModpackModel.MANIFEST, format = jsonEncodeDefaults)
            }
            else -> it.ignore()
        }
    }
}

fun cfMissingProjectsRule() = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.exportAsOverride(excludedProviders = setOf(CurseForge)) { bytesCallback, fileName, _ ->
                it.createFile(
                    bytesCallback,
                    OverrideType.OVERRIDE.folderName,
                    it.project.getPathStringWithSubpath(it.configFile),
                    fileName
                )
            }
        }
        else -> it.ignore()
    }
}

fun ExportingProject.addToCfModpackModel(projectFile: ProjectFile, modpackModel: CfModpackModel) =
    ruleResult("addToCfModpackModel ${project.type} ${project.slug}", Packaging.Action {
        projectFile.toCfModData(this.project)?.let { cfModData ->
            modpackModel.files.add(cfModData)
        }
        null // Return no error
    })

fun createCfModpackModel(
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
): CfModpackModel
{
    val cfLoaders: List<CfModLoaderData> = lockFile.getLoadersWithVersions().map { loader ->
        val (loaderName, loaderVersion) = loader

        CfModLoaderData(
            id = "$loaderName-$loaderVersion",
            primary = lockFile.getLoadersWithVersions().firstOrNull() == loader
        )
    }

    return CfModpackModel(
        CfMinecraftData(
            version = mcVersion,
            modLoaders = cfLoaders
        ),
        name = configFile.getName(),
        version = configFile.getVersion(),
        author = configFile.getAuthor(),
        files = mutableListOf()
    )
}

fun ProjectFile.toCfModData(parentProject: Project): CfModData?
{
    if (this.type != CurseForge.serialName) return null

    return CfModData(
        projectID = parentProject.id[CurseForge.serialName]!!.toInt(),
        fileID = this.id.toInt()
    )
}
