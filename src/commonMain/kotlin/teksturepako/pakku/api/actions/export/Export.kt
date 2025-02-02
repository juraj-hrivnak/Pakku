package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Result
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
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.debug
import teksturepako.pakku.io.*
import java.lang.Exception
import java.nio.file.Path
import kotlin.collections.fold
import kotlin.collections.map
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import com.github.michaelbull.result.fold as resultFold

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
    val overrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllOverrides()
    }

    val serverOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllServerOverrides()
    }

    val clientOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllClientOverrides()
    }

    profiles.map { profile ->
        launch {
            profile.build(exportingScope(lockFile, configFile)).export(
                onError = { profile, error -> onError(profile, error) },
                onSuccess = { profile, path, duration -> onSuccess(profile, path, duration) },
                lockFile, configFile, platforms, overrides, serverOverrides, clientOverrides
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
    overrides: Deferred<List<Result<String, ActionError>>>,
    serverOverrides: Deferred<List<Result<String, ActionError>>>,
    clientOverrides: Deferred<List<Result<String, ActionError>>>,
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
            .get() ?: return@measureTimedValue null

        // Create parent directory
        cacheDir.tryOrNull {
            it.createDirectory()
            it.setAttribute("dos:hidden", true)
        }

        val inputDirectory = Path(cacheDir.pathString, this.name)

        val results: List<RuleResult> = this.rules.filterNotNull().produceRuleResults(
            onError = { error -> onError(this, ExportingError(error)) },
            lockFile, configFile, this.name, overrides, serverOverrides, clientOverrides
        )

        // Run export rules
        val cachedPaths: List<Path> = results
            .resolveResults { error ->
                onError(this, error)
            }
            .awaitAll()
            .filterNotNull()
            .plus(
                results
                    .finishResults { error ->
                        onError(this, error)
                    }
                    .awaitAll()
                    .filterNotNull()
            )

        // -- FILE CACHE --

        /** Map of _absolute paths_ to their _hashes_ for every path not in a directory */
        val fileHashes: Map<Path, String> = cachedPaths.filterNot { it.isDirectory() }
            .mapNotNull { file ->
                file.tryToResult { it.readBytes() }
                    .onFailure { error ->
                        onError(this, IOExportingError(error))
                    }
                    .get()?.let { file to it }
            }
            .associate { it.first.absolute() to createHash("sha1", it.second) }

        /** Map of _absolute paths_ to their _hashes_ for every path in a directory */
        val dirContentHashes: Map<Path, String> = cachedPaths.filter { it.isDirectory() }
            .mapNotNull { directory ->
                directory.tryToResult { it.toFile().walkTopDown() }
                    .onFailure { error ->
                        onError(this, IOExportingError(error))
                    }.get()
            }
            .flatMap {
                it.mapNotNull { file -> file.toPath() }
            }
            .mapNotNull { path ->
                path.readAndCreateSha1FromBytes()?.let {
                    path.absolute() to it
                }
            }
            .toMap()

        val fileTreeWalk = inputDirectory.tryToResult { it.toFile().walkBottomUp() }
            .onFailure {error ->
                onError(this, IOExportingError(error))
            }.get()

        if (fileTreeWalk != null)
        {
            for (file in fileTreeWalk)
            {
                val path = file.toPath()
                if (path == inputDirectory) continue

                if (path.isDirectory())
                {
                    val currentDirFiles = path.tryToResult { it.toFile().listFiles() }
                        .onFailure { error ->
                            onError(this, IOExportingError(error))
                        }.get()?.mapNotNull { it.toPath() } ?: continue

                    if (currentDirFiles.isNotEmpty()) continue

                    val deleted = path.deleteIfExists()
                    if (deleted) debug { println("[${this.name}] CleanUp delete empty directory $path") }
                }
                else
                {
                    val hash = path.readAndCreateSha1FromBytes() ?: continue
                    if ((path.absolute() in fileHashes.keys && hash in fileHashes.values) ||
                        (path.absolute() in dirContentHashes.keys && hash in dirContentHashes.values)) continue

                    val deleted = path.deleteIfExists()
                    if (deleted) debug { println("[${this.name}] CleanUp delete file $path") }
                }
            }
        }

        // -- ZIP CREATION --

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

suspend fun List<RuleResult>.finishResults(
    onError: suspend (error: ActionError) -> Unit
): List<Deferred<Path?>> = coroutineScope {
    this@finishResults.mapNotNull { ruleResult ->
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
    onError: suspend (error: ActionError) -> Unit,
    lockFile: LockFile, configFile: ConfigFile, workingSubDir: String,
    overrides: Deferred<List<Result<String, ActionError>>>,
    serverOverrides: Deferred<List<Result<String, ActionError>>>,
    clientOverrides: Deferred<List<Result<String, ActionError>>>,
): List<RuleResult> = coroutineScope {

    val results = this@produceRuleResults.fold(listOf<Pair<ExportRule, RuleContext>>()) { acc, rule ->
        acc + lockFile.getAllProjects().map {
            rule to RuleContext.ExportingProject(it, lockFile, configFile, workingSubDir)
        } + overrides.await().mapNotNull { result ->
            result.resultFold(
                success = {
                    rule to RuleContext.ExportingOverride(it, OverrideType.OVERRIDE, lockFile, configFile, workingSubDir)
                },
                failure = {
                    onError(it)
                    null
                }
            )
        } + serverOverrides.await().mapNotNull { result ->
            result.resultFold(
                success = {
                    rule to RuleContext.ExportingOverride(it, OverrideType.SERVER_OVERRIDE, lockFile, configFile, workingSubDir)
                },
                failure = {
                    onError(it)
                    null
                }
            )
        } + clientOverrides.await().mapNotNull { result ->
            result.resultFold(
                success = {
                    rule to RuleContext.ExportingOverride(it, OverrideType.CLIENT_OVERRIDE, lockFile, configFile, workingSubDir)
                },
                failure = {
                    onError(it)
                    null
                }
            )
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
