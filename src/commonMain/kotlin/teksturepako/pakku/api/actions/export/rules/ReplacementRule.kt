package teksturepako.pakku.api.actions.export.rules

import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.RuleContext.Finished

fun replacementRule() = ExportRule {
    when (it)
    {
        is Finished ->
        {
            it.replaceText(
                "@name@" to it.configFile.getName(),
                "@version@" to it.configFile.getVersion(),
                "@description@" to it.configFile.getDescription(),
                "@author@" to it.configFile.getAuthor()
            )
        }
        else        -> it.ignore()
    }
}