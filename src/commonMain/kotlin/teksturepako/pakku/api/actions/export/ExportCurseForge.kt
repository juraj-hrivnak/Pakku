package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Ok
import teksturepako.pakku.api.actions.export.rules.ExportRule
import teksturepako.pakku.api.actions.export.rules.Packaging
import teksturepako.pakku.api.actions.export.rules.RuleContext.*
import teksturepako.pakku.api.actions.export.rules.ruleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.models.cf.CfModpackModel.*
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

fun exportCurseForge(modpackModel: CfModpackModel) = ExportRule {
    when (it)
    {
        is ExportingProject  ->
        {
            val projectFile = it.project.getFilesForPlatform(CurseForge).firstOrNull()

            if (projectFile == null)
            {
                it.setMissing()
            }
            else
            {
                it.addToCfModpackModel(projectFile, modpackModel)
            }
        }
        is ExportingOverride -> it.export()
        is MissingProject    -> it.exportAsOverrideFrom(Modrinth) { bytes, fileName, _ ->
            it.createFile(bytes, OverrideType.OVERRIDE.folderName, it.project.type.folderName, fileName)
        }
        is Finished          ->
        {
            it.createJsonFile(modpackModel, CfModpackModel.MANIFEST, format = jsonEncodeDefaults)
        }
    }
}

fun ExportingProject.addToCfModpackModel(projectFile: ProjectFile, modpackModel: CfModpackModel) =
    ruleResult("addToCfModpackModel ${project.type} ${project.slug}", Packaging.Action {
        Ok(projectFile.toCfModData(this.project)?.let { cfModData ->
            modpackModel.files.add(cfModData)
        })
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
            primary = lockFile.getLoadersWithVersions().firstOrNull()!! == loader
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
