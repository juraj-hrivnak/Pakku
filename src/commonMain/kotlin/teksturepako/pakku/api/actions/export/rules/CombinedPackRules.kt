package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.RuleContext.*

fun exportCombinedPack() = ExportRule {
    when (it)
    {
        is ExportingProject         ->
        {
            it.exportAsOverride(force = true) { bytesCallback, fileName, _ ->
                it.createFile(bytesCallback, it.project.getPathStringWithSubpath(it.configFile), fileName)
            }
        }
        is ExportingOverride        ->
        {
            it.export(overridesDir = null)
        }
        is ExportingProjectOverride ->
        {
            it.export(overridesDir = null)
        }
        else -> it.ignore()
    }
}