package teksturepako.pakku.api.actions.export

/**
 * A functional interface used to declare rules which control
 * what should happen with the content you want to export/package.
 *
 * These rules can be written in many ways and are fully extensible
 * by extending the [RuleContext] class.
 *
 * To create an export rule, you simply implement the functional interface like this:
 *
 * ```kotlin
 * fun exampleRule() = ExportRule {
 *     when (it)
 *     {
 *         is ExportingProject -> it.export() // (Sample function)
 *         else                -> it.ignore()
 *     }
 * }
 * ```
 *
 * Export rules must be part of an [export profile][ExportProfile] to be executed.
 */
fun interface ExportRule
{
    suspend fun getResult(ruleContext: RuleContext): RuleResult
}