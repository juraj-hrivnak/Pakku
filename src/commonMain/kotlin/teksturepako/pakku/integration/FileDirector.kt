package teksturepako.pakku.integration

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.NoFiles
import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.ExportingScope
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.actions.export.RuleContext.MissingProject
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.integration.FileDirectorModel.UrlEntry

@Serializable
data class FileDirectorModel(
    @SerialName("url") val urlBundle: MutableList<UrlEntry> = mutableListOf(),
    @SerialName("curse") val curseBundle: MutableList<CurseEntry> = mutableListOf()
)
{
    @Serializable
    data class UrlEntry(
        val url: String, val folder: String, val fileName: String
    )

    @Serializable
    data class CurseEntry(
        val addonId: String, val fileId: String, val folder: String, val fileName: String
    )
}

fun ExportingScope.fileDirectorRule(
    excludedProviders: Set<Provider> = setOf(),
    fileDirectorModel: FileDirectorModel = FileDirectorModel()
): ExportRule?
{
    if (lockFile.getProject("filedirector") == null || lockFile.getProject("autopack-director") == null) return null

    return ExportRule {
        when (it)
        {
            is MissingProject ->
            {
                it.addToFileDirector(fileDirectorModel, excludedProviders)
            }
            is Finished       ->
            {
                it.createJsonFile(fileDirectorModel, "overrides/config/mod-director/.bundle.json")
            }
            else              -> it.ignore()
        }
    }
}

data class CanNotAddToFileDirector(val project: Project) : ActionError()
{
    override val rawMessage = "${project.slug} can not be added to FileDirector's config, because it is not redistributable."

    override fun message(arg: String) = "${project.getFlavoredSlug()} can not be added to FileDirector's config," +
        " because it is not redistributable."
}

fun MissingProject.addToFileDirector(
    fileDirector: FileDirectorModel, excludedProviders: Set<Provider> = setOf()
) = ruleResult("addToFileDirector ${project.slug}", Packaging.Action {
    if (!project.redistributable) return@Action CanNotAddToFileDirector(project)

    val (projectFile, url) = (Provider.providers - excludedProviders).firstNotNullOfOrNull { provider ->
        val projectFile = project.getFilesForProvider(provider).firstOrNull()

        projectFile?.url?.let {
            projectFile to UrlEncoderUtil.encode(it, ":/") // Encode the URL due to bug in FileDirector.
        }
    } ?: return@Action NoFiles(project, lockFile)

    fileDirector.urlBundle.plusAssign(
        UrlEntry(
            url = url,
            folder = this.project.getPathStringWithSubpath(this.configFile),
            fileName = projectFile.fileName
        )
    )

    null
})
