package teksturepako.pakku.api.actions.export

@ExportSystemDsl
@Suppress("FunctionName")
fun ExportRuleScope.OptionalExportRule(condition: Boolean, exportRule: ExportRule): ExportRule?
{
    return if (condition) exportRule else null
}

@ExportSystemDsl
@Suppress("FunctionName")
fun ExportRuleScope.OptionalExportRule(requiresProject: String, exportRule: ExportRule): ExportRule?
{
    return if (lockFile.getProject(requiresProject) != null) exportRule else null
}
