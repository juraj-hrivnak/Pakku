package teksturepako.pakku.api.actions.export.rules


fun interface ExportRule
{
    suspend fun getResult(ruleContext: RuleContext): RuleResult
}