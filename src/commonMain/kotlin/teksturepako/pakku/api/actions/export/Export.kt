package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.export.Packaging.*
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.debug
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.tryToResult
import teksturepako.pakku.io.zip
import java.nio.file.Path
import kotlin.io.path.*

suspend fun export(
    profiles: List<ExportProfile>,
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile
): List<Job> = coroutineScope {
    profiles.map { profile ->
        launch {
            profile.export(
                onError = { profile, error -> onError(profile, error) },
                onSuccess = { profile, path -> onSuccess(profile, path) },
                lockFile, configFile
            )
        }
    }
}

suspend fun ExportProfile.export(
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile
)
{
    val inputDirectory = Path(workingPath, PAKKU_DIR, "temp", this.name)

    val results = this.rules.filterNotNull()
        .produceRuleResults(lockFile, configFile, this.name)

    val files = results.resolveResults { onError(this, it) }.awaitAll().filterNotNull() +
            results.finishResults { onError(this, it) }.awaitAll().filterNotNull()

    val fileHashes = files
        .filterNot { it.isDirectory() }
        .mapNotNull { file ->
            file.tryToResult { it.readBytes() }.onFailure { onError(this, it) }.get()
        }
        .map { createHash("sha1", it) }

    val dirContentHashes = files
        .filter { it.isDirectory() }
        .mapNotNull { file ->
            file.tryToResult { it.toFile().walkTopDown() }.onFailure { onError(this, it) }.get()
        }
        .flatMap { fileTreeWalk ->
            fileTreeWalk.mapNotNull { it.toPath() }
        }
        .mapNotNull { it.generateSHA1FromBytes() }

    val fileTreeWalk = inputDirectory
        .tryToResult { it.toFile().walkTopDown() }
        .onFailure { onError(this, it) }.get()

    if (fileTreeWalk != null)
    {
        for (file in fileTreeWalk)
        {
            val path = file.toPath()

            if (path.isDirectory()) continue

            val hash = path.generateSHA1FromBytes() ?: continue

            if (hash in fileHashes || hash in dirContentHashes) continue

            val deleted = path.deleteIfExists()
            if (deleted) debug { println("[${this.name}] CleanUp delete $path") }
        }
    }

    val modpackName = when
    {
        configFile.getName().isBlank()       -> "Modpack"
        configFile.getVersion().isNotBlank() -> "${configFile.getName()}-${configFile.getVersion()}"
        else -> configFile.getName()
    }

    val outputZipFile = Path(workingPath, "build", this.name, "$modpackName.${this.fileExtension}")
    outputZipFile.tryToResult { it.createParentDirectories() }.onFailure { onError(this, it) }

    zip(inputDirectory, outputZipFile)

    onSuccess(this, outputZipFile)
}

private suspend fun Path.generateSHA1FromBytes() = this.tryToResult { it.readBytes() }.get()
    ?.let { createHash("sha1", it) }

suspend fun List<RuleResult>.resolveResults(
    onError: suspend (error: ActionError) -> Unit
): List<Deferred<Path?>> = coroutineScope {
    this@resolveResults.mapNotNull { ruleResult ->
        when (val packagingAction = ruleResult.packaging)
        {
            is Error        ->
            {
                onError(packagingAction.error)
                debug { println(ruleResult) }
                null
            }
            is Ignore       -> null
            is EmptyAction  ->
            {
                debug { println(ruleResult) }
                null
            }
            is Action       ->
            {
                if (ruleResult.ruleContext !is Finished)
                {
                    async {
                        packagingAction.action()?.let {
                            onError(it)
                        }
                        null
                    }.also {
                        it.invokeOnCompletion {
                            debug { println(ruleResult) }
                        }
                    }
                }
                else null
            }
            is FileAction   ->
            {
                if (ruleResult.ruleContext !is Finished)
                {
                    async(Dispatchers.IO) {
                        packagingAction.action().let { (file, error) ->
                            if (error != null) onError(error)
                            file
                        }
                    }.also {
                        it.invokeOnCompletion {
                            debug { println(ruleResult) }
                        }
                    }
                }
                else null
            }
        }
    }
}

suspend fun List<RuleResult>.finishResults(
    onError: suspend (error: ActionError) -> Unit
): List<Deferred<Path?>> = coroutineScope {
    this@finishResults.mapNotNull { ruleResult ->
        when
        {
            ruleResult.ruleContext is Finished && ruleResult.packaging is Action    ->
            {
                async {
                    ruleResult.packaging.action()?.let {
                        onError(it)
                    }
                    null
                }.also {
                    it.invokeOnCompletion {
                        debug { println(ruleResult) }
                    }
                }
            }
            ruleResult.ruleContext is Finished && ruleResult.packaging is FileAction ->
            {
                async(Dispatchers.IO) {
                    ruleResult.packaging.action().let { (file, error) ->
                        if (error != null) onError(error)
                        file
                    }
                }.also {
                    it.invokeOnCompletion {
                        debug { println(ruleResult) }
                    }
                }
            }
            else -> null
        }
    }
}

suspend fun List<ExportRule>.produceRuleResults(
    lockFile: LockFile, configFile: ConfigFile, workingSubDir: String
): List<RuleResult>
{
    val results = this.fold(listOf<Pair<ExportRule, RuleContext>>()) { acc, rule ->
        acc + lockFile.getAllProjects().map {
            rule to RuleContext.ExportingProject(it, lockFile, configFile, workingSubDir)
        } + configFile.getAllOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.OVERRIDE, lockFile, configFile, workingSubDir)
        } + configFile.getAllServerOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.SERVER_OVERRIDE, lockFile, configFile, workingSubDir)
        } + configFile.getAllClientOverrides().map {
            rule to RuleContext.ExportingOverride(it, OverrideType.CLIENT_OVERRIDE, lockFile, configFile, workingSubDir)
        } + readProjectOverrides().map {
            rule to RuleContext.ExportingProjectOverride(it, lockFile, configFile, workingSubDir)
        }
    }.map { (rule, ruleEntry) ->
        rule.getResult(ruleEntry)
    }

    val missing = results.filter {
        it.ruleContext is RuleContext.MissingProject
    }.flatMap { ruleResult ->
        this.map { rule ->
            val project = (ruleResult.ruleContext as RuleContext.MissingProject).project
            rule.getResult(RuleContext.MissingProject(project, lockFile, configFile, workingSubDir))
        }
    }

    val finished = this.map { rule ->
        rule.getResult(Finished(workingSubDir))
    }

    return results + missing + finished
}