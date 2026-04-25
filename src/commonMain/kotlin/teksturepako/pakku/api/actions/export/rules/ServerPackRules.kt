package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.RuleContext.*
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.CurseForge

fun serverPackRule() = ExportRule {
    when (it)
    {
        is ExportingProject         ->
        {
            if (OverrideType.fromProject(it.project) !in listOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE))
            {
                return@ExportRule it.ignore()
            }

            it.project.getLatestFile(setOf(CurseForge))
                ?: return@ExportRule it.setMissing()

            it.exportAsOverride(force = true) { bytesCallback, fileName, _ ->
                it.createFile(bytesCallback, it.project.getPathStringWithSubpath(it.configFile), fileName)
            }
        }
        is MissingProject ->
        {
            it.exportAsOverride(excludedProviders = setOf(CurseForge)) { bytesCallback, fileName, _ ->
                it.createFile(
                    bytesCallback,
                    it.project.getPathStringWithSubpath(it.configFile),
                    fileName
                )
            }
        }
        is ExportingOverride       ->
        {
            if (it.type !in listOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE))
            {
                it.ignore()
            }
            else
            {
                it.export(overridesDir = null)
            }
        }
        is ExportingManualOverride ->
        {
            if (it.manualOverride.type !in listOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE))
            {
                it.ignore()
            }
            else
            {
                it.export(overridesDir = null)
            }
        }
        else -> it.ignore()
    }
}