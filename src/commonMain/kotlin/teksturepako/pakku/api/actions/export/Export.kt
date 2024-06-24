package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.export.rules.ExportRule
import teksturepako.pakku.api.actions.export.rules.Packaging.*
import teksturepako.pakku.api.actions.export.rules.RuleContext
import teksturepako.pakku.api.actions.export.rules.RuleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.debug
import teksturepako.pakku.io.tryToResult
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
suspend inline fun export(
    rules: List<ExportRule?>,
    crossinline onError: (error: ActionError) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile
)
{
    Path(workingPath, PAKKU_DIR, "temp").tryToResult { it.deleteRecursively() }
        .onFailure { onError(it) }

    val results = rules.filterNotNull()
        .produceRuleResults(lockFile, configFile)

    results.resolveResults { onError(it) }.joinAll()
    results.finishResults { onError(it) }.joinAll()

    val inputDirectory = Path(workingPath, PAKKU_DIR, "temp").toFile()
    val outputZipFile = Path(workingPath, PAKKU_DIR, "out.zip").toFile()
    withContext(Dispatchers.IO) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            inputDirectory.walkTopDown().forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(inputDirectory.absolutePath).removePrefix("/")
                val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                zos.putNextEntry(entry)
                if (file.isFile)
                {
                    file.inputStream().use { fis -> fis.copyTo(zos) }
                }
            }
        }
    }
}

suspend fun List<RuleResult>.resolveResults(
    onError: (error: ActionError) -> Unit
) = coroutineScope {
    this@resolveResults.mapNotNull { ruleResult ->
        when (val packagingAction = ruleResult.packaging)
        {
            is Ignore  -> null
            is EmptyAction ->
            {
                debug { println(ruleResult) }
                null
            }
            is Action  ->
            {
                if (ruleResult.ruleContext !is RuleContext.Finished)
                {
                    debug { println(ruleResult) }
                    launch {
                        packagingAction.action().onFailure {
                            onError(it)
                        }
                    }
                }
                else null
            }
        }
    }
}

suspend fun List<RuleResult>.finishResults(
    onError: (error: ActionError) -> Unit
) = coroutineScope {
    this@finishResults.mapNotNull { ruleResult ->
        if (ruleResult.ruleContext is RuleContext.Finished && ruleResult.packaging is Action)
        {
            debug { println(ruleResult) }
            launch {
                ruleResult.packaging.action().onFailure {
                    onError(it)
                }
            }
        }
        else null
    }
}

suspend fun List<ExportRule>.produceRuleResults(
    lockFile: LockFile, configFile: ConfigFile
): List<RuleResult>
{
    val results = this.fold(listOf<Pair<ExportRule, RuleContext>>()) { acc, rule ->
        acc + lockFile.getAllProjects().map {
            rule to RuleContext.ExportingProject(it, lockFile, configFile)
        } + configFile.getAllOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.OVERRIDE, lockFile, configFile)
        } + configFile.getAllServerOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.SERVER_OVERRIDE, lockFile, configFile)
        } + configFile.getAllClientOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.CLIENT_OVERRIDE, lockFile, configFile)
        }
    }.map { (rule, ruleEntry) ->
        rule.getResult(ruleEntry)
    }

    val missing = results.filter {
        it.ruleContext is RuleContext.MissingProject
    }.flatMap { ruleResult ->
        this.map { rule ->
            rule.getResult(
                RuleContext.MissingProject(
                    (ruleResult.ruleContext as RuleContext.MissingProject).project, lockFile, configFile
                )
            )
        }
    }

    val finished = this.map { rule ->
        rule.getResult(RuleContext.Finished)
    }

    return results + missing + finished
}