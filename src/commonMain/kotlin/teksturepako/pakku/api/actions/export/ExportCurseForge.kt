package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.actions.export.Rule.Finished
import teksturepako.pakku.api.actions.export.Rule.Entry
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile

fun exportCurseForge(modpackModel: CfModpackModel) = ExportRule { rule ->
    when (rule)
    {
        is Entry    ->
        {
            val projectFile = rule.project.getFilesForPlatform(rule.platform).firstOrNull()

            if (projectFile == null)
            {
                val projectFile2 = rule.project.getFilesForPlatform(Modrinth).firstOrNull()
                    ?: return@ExportRule rule.ignore()

                val bytes = projectFile2.url?.let { Http().requestByteArray(it) }
                    ?: return@ExportRule rule.ignore()

                return@ExportRule rule.createFile(bytes, projectFile2.fileName)
            }

            rule.export { modpackModel.files.add(projectFile.toCfModData(rule.project)) }
        }
        is Finished ->
        {
            rule.createJsonFile(modpackModel, CfModpackModel.MANIFEST, jsonEncodeDefaults)
        }
    }
}

fun createCfModpackModel(
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
): CfModpackModel
{
    val cfLoaders: List<CfModpackModel.CfModLoaderData> = lockFile.getLoadersWithVersions()
        .map { dep ->
            CfModpackModel.CfModLoaderData(
                id = "${dep.first}-${dep.second}",
                primary = lockFile.getLoadersWithVersions().firstOrNull()!! == dep
            )
        }

    return CfModpackModel(
        CfModpackModel.CfMinecraftData(
            version = mcVersion, modLoaders = cfLoaders
        ),
        name = configFile.getName(),
        version = configFile.getVersion(),
        author = configFile.getAuthor(),
        files = mutableListOf()
    )
}

fun ProjectFile.toCfModData(parentProject: Project): CfModpackModel.CfModData
{
    return CfModpackModel.CfModData(
        projectID = parentProject.id[CurseForge.serialName]!!.toInt(),
        fileID = this.id.toInt()
    )
}
