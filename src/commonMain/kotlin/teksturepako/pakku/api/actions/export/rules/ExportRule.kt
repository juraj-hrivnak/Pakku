package teksturepako.pakku.api.actions.export.rules

/** An interface used to declare rules when exporting a modpack. */
fun interface ExportRule
{
    suspend fun getResult(ruleContext: RuleContext): RuleResult
}