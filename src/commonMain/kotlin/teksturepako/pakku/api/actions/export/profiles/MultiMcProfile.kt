package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.RuleContext
import teksturepako.pakku.api.actions.export.exportProfile

fun multiMcProfile() = exportProfile(name = "multimc") {
    rule { multiMcModpackRule() }
}

fun multiMcModpackRule() = ExportRule {
    when (it)
    {
        is RuleContext.ExportingProject -> it.exportAsOverride { bytesCallback, fileName, _ ->
            it.createFile(bytesCallback, ".minecraft", it.project.getPathStringWithSubpath(it.configFile), fileName)
        }
        is RuleContext.ExportingOverride -> it.export(".minecraft")
        is RuleContext.ExportingProjectOverride -> it.export(".minecraft")
        else -> it.ignore()
    }
}