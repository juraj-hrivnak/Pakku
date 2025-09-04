package teksturepako.pakku.integration.autopackdirector

import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.NoFiles
import teksturepako.pakku.api.actions.export.ExportRuleScope
import teksturepako.pakku.api.actions.export.OptionalExportRule
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.actions.export.RuleContext.MissingProject
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.integration.filedierector.models.FileDirectorModel
import teksturepako.pakku.integration.filedierector.models.FileDirectorModel.UrlEntry

fun ExportRuleScope.autoPackDirectorRule(
    excludedProviders: Set<Provider> = setOf(),
    fileDirectorModel: FileDirectorModel = FileDirectorModel()
) = OptionalExportRule(requiresProject = "autopack-director") {
    when (it)
    {
        is MissingProject ->
        {
            it.addToAutoPackDirector(fileDirectorModel, excludedProviders)
        }
        is Finished       ->
        {
            it.createJsonFile(fileDirectorModel, "overrides/config/mod-director/.bundle.json")
        }
        else              -> it.ignore()
    }
}

data class CanNotAddToAutoPackDirector(val project: Project) : ActionError()
{
    override val rawMessage = "${project.slug} can not be added to AutoPack-Director's config, because it is not redistributable."

    override fun message(arg: String) = "${project.getFlavoredSlug()} can not be added to AutoPack-Director's config," +
        " because it is not redistributable."
}

private fun MissingProject.addToAutoPackDirector(
    fileDirector: FileDirectorModel, excludedProviders: Set<Provider> = setOf()
) = ruleResult("addToAutoPackDirector ${project.slug}", Packaging.Action {
    if (!project.redistributable) return@Action CanNotAddToAutoPackDirector(project)

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
