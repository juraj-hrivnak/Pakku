package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.actions.export.rules.ExportRule
import teksturepako.pakku.api.actions.export.rules.RuleContext.MissingProject
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.Platform

fun exportMissingProjects(platform: Platform) = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.exportAsOverrideFrom(platform) { bytesCallback, fileName, _ ->
                it.createFile(bytesCallback, OverrideType.OVERRIDE.folderName, it.project.type.folderName, fileName)
            }
        }
        else -> it.ignore()
    }
}