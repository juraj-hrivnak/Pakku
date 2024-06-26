package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.*
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel.MrFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide

fun ruleOfMrModpack(modpackModel: MrModpackModel) = ExportRule {
    when (it)
    {
        is ExportingProject         ->
        {
            val projectFile = it.project.getFilesForPlatform(Modrinth).firstOrNull()
                ?: return@ExportRule it.setMissing()

            it.addToMrModpackModel(projectFile, modpackModel)
        }
        is ExportingOverride        -> it.export()
        is ExportingProjectOverride -> it.export()
        is Finished                 ->
        {
            it.createJsonFile(modpackModel, MrModpackModel.MANIFEST, format = jsonEncodeDefaults)
        }
        else                        -> it.ignore()
    }
}

fun ruleOfMrMissingProjects(platform: Platform) = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.exportAsOverrideFrom(platform) { bytesCallback, fileName, overridesDir ->
                it.createFile(bytesCallback, overridesDir, it.project.type.folderName, fileName)
            }
        }
        else -> it.ignore()
    }
}

fun ExportingProject.addToMrModpackModel(projectFile: ProjectFile, modpackModel: MrModpackModel) =
    ruleResult("addToMrModpackModel ${project.type} ${project.slug}", Packaging.Action {
        projectFile.toMrFile(this.project)?.let { mrFile ->
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

fun ProjectFile.toMrFile(parentProject: Project): MrFile?
{
    if (this.type != Modrinth.serialName) return null

    val client = if (parentProject.side == ProjectSide.CLIENT || parentProject.side == ProjectSide.BOTH)
    {
        "required"
    }
    else "unsupported"

    val server = if (parentProject.side == ProjectSide.SERVER || parentProject.side == ProjectSide.BOTH)
    {
        "required"
    }
    else "unsupported"

    return MrFile(
        path = "${parentProject.type.folderName}/${this.fileName}",
        hashes = MrFile.Hashes(
            sha512 = this.hashes?.get("sha512")!!,
            sha1 = this.hashes["sha1"]!!
        ),
        env = MrFile.Env(
            client = client,
            server = server,
        ),
        // Replace ' ' in URL with '+'
        downloads = setOf(this.url!!.replace(" ", "+")),
        fileSize = this.size
    )
}