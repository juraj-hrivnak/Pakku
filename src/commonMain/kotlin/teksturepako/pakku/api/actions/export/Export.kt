package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.export.PackagingAction.Action
import teksturepako.pakku.api.actions.export.PackagingAction.Ignore
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.debug

suspend inline fun Platform.export(
    rules: List<ExportRule>,
    crossinline onError: (error: ActionError) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile
) = coroutineScope {
    val ruleResults = lockFile.getAllProjects()
        .flatMap { project ->
            rules.map { rule ->
                async {
                    rule to Rule.Entry(project, lockFile, configFile,this@export)
                }
            }
        }
        .awaitAll()
        .plus(
            rules.map { rule ->
                rule to Rule.Finished
            }
        )
        .map { (rule, ruleEntry) ->
            rule.onExport(ruleEntry)
        }

    ruleResults.forEach { ruleResult ->
        when (val packagingAction = ruleResult.packagingAction)
        {
            is Action ->
            {
                packagingAction.action()

                debug { println(ruleResult) }
            }
            is Ignore    ->
            {

            }
        }
    }
}