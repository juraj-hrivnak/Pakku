package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.actions.export.Packaging.*
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.actions.export.profiles.defaultProfiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.cacheDir
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.OverridesDeferred
import teksturepako.pakku.api.overrides.getOverridesAsync
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.debug
import teksturepako.pakku.io.cleanUpDirectory
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.tryToResult
import teksturepako.pakku.io.zip
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.measureTimedValue

suspend fun exportDefaultProfiles(
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path, duration: Duration) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>
): List<Job>
{
    return export(
        profiles = defaultProfiles,
        onError = { profile, error -> onError(profile, error) },
        onSuccess = { profile, path, duration -> onSuccess(profile, path, duration) },
        lockFile, configFile, platforms
    )
}

suspend fun export(
    profiles: List<ExportProfileBuilder>,
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path, duration: Duration) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>
): List<Job> = coroutineScope {
    val overrides = getOverridesAsync(configFile)

    profiles.map { profile ->
        launch {
            profile.build(exportingScope(lockFile, configFile)).export(
                onError = { profile, error -> onError(profile, error) },
                onSuccess = { profile, path, duration -> onSuccess(profile, path, duration) },
                lockFile, configFile, platforms, overrides
            )
        }
    }
}

suspend fun ExportProfile.export(
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path, duration: Duration) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>,
    overrides: OverridesDeferred
)
{
    if (this.requiresPlatform != null && this.requiresPlatform !in platforms) return

    val timedValue = measureTimedValue {

        val modpackFileName = when
        {
            configFile.getName().isBlank() -> "Modpack"
            configFile.getVersion().isNotBlank() -> "${configFile.getName()}-${configFile.getVersion()}"
            else -> configFile.getName()
        } + ".${this.fileExtension}"

        val outputZipFile = runCatching { Path(workingPath, "build", this.name, modpackFileName) }
            .onFailure { e: Throwable ->
                onError(this, CouldNotExport(this, modpackFileName, e.message))
            }
            .get()
            ?: return@measureTimedValue null

        // Create parent directory
        cacheDir.tryOrNull {
            it.createDirectories()
            it.setAttribute("dos:hidden", true)
        }

        val inputDirectory = Path(cacheDir.pathString, this.name)

        val results: List<RuleResult> = this.rules
            .filterNotNull()
            .produceRuleResults(lockFile, configFile, this.name, overrides)

        val cachedPaths: List<Path> = results
            .runEffects { error ->
                onError(this, error)
            }
            .awaitAll()
            .filterNotNull() + results
                .runEffectsOnFinished { error ->
                    onError(this, error)
                }
                .awaitAll()
                .filterNotNull()

        cleanUpDirectory(
            inputDirectory, cachedPaths,
            onError = { onError(this, IOExportingError(it)) },
            onAction = { action -> debug { println("[${this.name}] CleanUp $action") } }
        )

        outputZipFile
            .tryToResult { it.createParentDirectories() }
            .onFailure { error ->
                if (error !is AlreadyExists)
                {
                    onError(this, CouldNotExport(this, modpackFileName, error.rawMessage))
                    return@measureTimedValue null
                }
            }

        try
        {
            zip(inputDirectory, outputZipFile)
        }
        catch (e: Exception)
        {
            onError(this, CouldNotExport(this, modpackFileName, e.stackTraceToString()))
            return@measureTimedValue null
        }

        outputZipFile
    }

    if (timedValue.value != null)
    {
        onSuccess(this, timedValue.value!!, timedValue.duration)
    }
}

suspend fun List<RuleResult>.runEffects(
    onError: suspend (error: ActionError) -> Unit
): List<Deferred<Path?>> = coroutineScope {
    this@runEffects.mapNotNull { ruleResult ->
        when (val packagingAction = ruleResult.packaging)
        {
            is Error        ->
            {
                onError(packagingAction.error)
                debug { println(ruleResult) }
                if (packagingAction.error.severity == ErrorSeverity.FATAL)
                {
                    debug { println("FATAL") }
                    return@coroutineScope listOf()
                }

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
                    val action = measureTimedValue {
                        async {
                            packagingAction.action()?.let {
                                onError(ExportingError(it))
                            }
                        }
                    }

                    action.value.invokeOnCompletion {
                        debug { println("$ruleResult in ${action.duration}") }
                    }

                    null
                }
                else null
            }
            is FileAction   ->
            {
                if (ruleResult.ruleContext !is Finished)
                {
                    val action = measureTimedValue {
                        async(Dispatchers.IO) {
                            packagingAction.action().let { (file, error) ->
                                if (error != null) onError(IOExportingError(error))
                                file
                            }
                        }
                    }

                    action.value.invokeOnCompletion {
                        debug { println("$ruleResult in ${action.duration}") }
                    }

                    action.value
                }
                else null
            }
        }
    }
}

suspend fun List<RuleResult>.runEffectsOnFinished(
    onError: suspend (error: ActionError) -> Unit
): List<Deferred<Path?>> = coroutineScope {
    this@runEffectsOnFinished.mapNotNull { ruleResult ->
        when
        {
            ruleResult.ruleContext is Finished && ruleResult.packaging is Action    ->
            {
                val action = measureTimedValue {
                    async {
                        ruleResult.packaging.action()?.let {
                            onError(ExportingError(it))
                        }
                    }
                }

                action.value.invokeOnCompletion {
                    debug { println("$ruleResult in ${action.duration}") }
                }

                null
            }
            ruleResult.ruleContext is Finished && ruleResult.packaging is FileAction ->
            {
                val action = measureTimedValue {
                    async(Dispatchers.IO) {
                        ruleResult.packaging.action().let { (file, error) ->
                            if (error != null) onError(IOExportingError(error))
                            file
                        }
                    }
                }

                action.value.invokeOnCompletion {
                    debug { println("$ruleResult in ${action.duration}") }
                }

                action.value
            }
            else -> null
        }
    }
}

/**
 * Runs through a list of [ExportRules][ExportRule],
 * applies data to their [RuleContexts][RuleContext] and transforms them into [RuleResults][RuleResult].
 *
 * [RuleContext.MissingProject] and [RuleContext.Finished] are applied last.
 */
suspend fun List<ExportRule>.produceRuleResults(
    lockFile: LockFile, configFile: ConfigFile, workingSubDir: String, overrides: OverridesDeferred
): List<RuleResult> = coroutineScope {

    val results = this@produceRuleResults.fold(listOf<Pair<ExportRule, RuleContext>>()) { acc, rule ->
        acc + lockFile.getAllProjects().map {
            rule to RuleContext.ExportingProject(it, lockFile, configFile, workingSubDir)
        } + overrides.awaitAll().map { (overridePath, overrideType) ->
            rule to RuleContext.ExportingOverride(overridePath, overrideType, lockFile, configFile, workingSubDir)
        } + readProjectOverrides(configFile).map {
            rule to RuleContext.ExportingProjectOverride(it, lockFile, configFile, workingSubDir)
        }
    }.map { (exportRule, ruleContext) ->
        exportRule.getResult(ruleContext)
    }

    val missing = results.filter {
        it.ruleContext is RuleContext.MissingProject
    }.flatMap { ruleResult ->
        this@produceRuleResults.map { rule ->
            val project = (ruleResult.ruleContext as RuleContext.MissingProject).project
            rule.getResult(RuleContext.MissingProject(project, lockFile, configFile, workingSubDir))
        }
    }

    val finished = this@produceRuleResults.map { rule ->
        rule.getResult(Finished(lockFile, configFile, workingSubDir))
    }

    return@coroutineScope results + missing + finished
}
