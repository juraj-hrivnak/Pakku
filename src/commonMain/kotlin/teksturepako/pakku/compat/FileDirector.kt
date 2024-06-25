package teksturepako.pakku.compat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.NoFiles
import teksturepako.pakku.api.actions.export.rules.ExportRule
import teksturepako.pakku.api.actions.export.rules.Packaging
import teksturepako.pakku.api.actions.export.rules.RuleContext.Finished
import teksturepako.pakku.api.actions.export.rules.RuleContext.MissingProject
import teksturepako.pakku.api.actions.export.rules.RuleResult
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.compat.FileDirectorModel.UrlEntry

@Serializable
data class FileDirectorModel(
    @SerialName("url") val urlBundle: MutableList<UrlEntry> = mutableListOf(),
    @SerialName("curse") val curseBundle: MutableList<CurseEntry> = mutableListOf()
)
{
    @Serializable
    data class UrlEntry(
        val url: String, val folder: String
    )

    @Serializable
    data class CurseEntry(
        val addonId: String, val fileId: String, val folder: String
    )
}

fun exportFileDirector(fileDirectorModel: FileDirectorModel = FileDirectorModel()) = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.addToFileDirector(fileDirectorModel, Modrinth)
        }
        is Finished       ->
        {
            it.createJsonFile(fileDirectorModel, "overrides/config/mod-director/.bundle.json")
        }
        else              -> it.ignore()
    }
}

data class CouldNotAddToFileDirector(val project: Project) :
    ActionError("${project.slug} could not be added to file director config.")

fun MissingProject.addToFileDirector(fileDirector: FileDirectorModel, platform: Platform) =
    RuleResult("addToFileDirector", this, Packaging.Action {
        if (!project.redistributable) return@Action CouldNotAddToFileDirector(project)

        val url = project.getFilesForPlatform(platform).firstOrNull()?.url
            ?: return@Action NoFiles(project, lockFile)

        fileDirector.urlBundle.plusAssign(
            UrlEntry(
                url = url.replace(" ", "+"),
                folder = this.project.type.folderName
            )
        )

        null
    })
