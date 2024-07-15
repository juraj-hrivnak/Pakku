package teksturepako.pakku.compat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.NoFiles
import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.actions.export.RuleContext.MissingProject
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.ui.getFlavoredSlug
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

fun exportFileDirector(
    platform: Platform,
    fileDirectorModel: FileDirectorModel = FileDirectorModel()
) = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.addToFileDirector(fileDirectorModel, platform)
        }
        is Finished       ->
        {
            it.createJsonFile(fileDirectorModel, "overrides/config/mod-director/.bundle.json")
        }
        else              -> it.ignore()
    }
}

data class CanNotAddToFileDirector(val project: Project) :
    ActionError("${project.slug} can not be added to FileDirector's config, because it is not redistributable.")
    {
        override fun message(arg: String) = "${project.getFlavoredSlug()} can not be added to FileDirector's config," +
                " because it is not redistributable."
    }

fun MissingProject.addToFileDirector(fileDirector: FileDirectorModel, platform: Platform) =
    ruleResult("addToFileDirector ${project.slug}", Packaging.Action {
        if (!project.redistributable) return@Action CanNotAddToFileDirector(project)

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
