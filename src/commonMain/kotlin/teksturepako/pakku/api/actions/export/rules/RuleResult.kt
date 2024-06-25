package teksturepako.pakku.api.actions.export.rules

data class RuleResult(
    val message: String,
    val ruleContext: RuleContext,
    val packaging: Packaging,
)
{
    override fun toString(): String = buildString {
        append("[${ruleContext.workingSubDir}]")
        append(" ")
        append(packaging::class.simpleName)
        append(" ")
        append(ruleContext::class.simpleName)
        append(" ")
        append(message)
    }
}

fun RuleContext.ruleResult(message: String, packaging: Packaging) =
    RuleResult(message, this, packaging)