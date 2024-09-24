package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.CouldNotSave
import teksturepako.pakku.api.actions.export.Packaging.*
import teksturepako.pakku.api.actions.export.RuleContext.Finished
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.cacheDir
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.debug
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.tryToResult
import teksturepako.pakku.io.zip
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.measureTimedValue


suspend fun export(
    profiles: List<ExportProfile>,
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path, duration: Duration) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>
): List<Job> = coroutineScope {
    profiles.map { profile ->
        launch {
            profile.export(
                onError = { profile, error -> onError(profile, error) },
                onSuccess = { profile, path, duration -> onSuccess(profile, path, duration) },
                lockFile, configFile, platforms
            )
        }
    }
}

suspend fun ExportProfile.export(
    onError: suspend (profile: ExportProfile, error: ActionError) -> Unit,
    onSuccess: suspend (profile: ExportProfile, path: Path, duration: Duration) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile,
    platforms: List<Platform>
)
{
    if (this.dependsOn != null && this.dependsOn !in platforms) return

    val timedValue = measureTimedValue {

        val modpackFileName = when
        {
            configFile.getName().isBlank() -> "Modpack"
            configFile.getVersion().isNotBlank() -> "${configFile.getName()}-${configFile.getVersion()}"
            else -> configFile.getName()
        } + ".${this.fileExtension}"

        val outputZipFile = runCatching { Path(workingPath, "build", this.name, modpackFileName) }
            .onFailure { onError(this, CouldNotSave(null, it.message)) }
            .get() ?: return

        // Create parent directory
        cacheDir.tryOrNull {
            it.createDirectory()
            it.setAttribute("dos:hidden", true)
        }

        val inputDirectory = Path(cacheDir.pathString, this.name)

        val results: List<RuleResult> = this.rules.filterNotNull().produceRuleResults(lockFile, configFile, this.name)

        // Run export rules
        val cachedPaths: List<Path> = results.resolveResults { onError(this, it) }.awaitAll().filterNotNull() +
                results.finishResults { onError(this, it) }.awaitAll().filterNotNull()

        // -- FILE CACHE --

        /** Map of _absolute paths_ to their _hashes_ for every path not in a directory */
        val fileHashes: Map<Path, String> = cachedPaths.filterNot { it.isDirectory() }
            .mapNotNull { file ->
                file.tryToResult { it.readBytes() }
                    .onFailure { onError(this, it) }
                    .get()?.let { file to it }
            }
            .associate { it.first.absolute() to createHash("sha1", it.second) }

        /** Map of _absolute paths_ to their _hashes_ for every path in a directory */
        val dirContentHashes: Map<Path, String> = cachedPaths.filter { it.isDirectory() }
            .mapNotNull { directory ->
                directory.tryToResult { it.toFile().walkTopDown() }
                    .onFailure { onError(this, it) }.get()
            }
            .flatMap {
                it.mapNotNull { file -> file.toPath() }
            }
            .mapNotNull { path ->
                path.generateSHA1FromBytes()?.let {
                    path.absolute() to it
                }
            }
            .toMap()

        val fileTreeWalk = inputDirectory.tryToResult { it.toFile().walkBottomUp() }
            .onFailure { onError(this, it) }.get()

        if (fileTreeWalk != null)
        {
            for (file in fileTreeWalk)
            {
                val path = file.toPath()
                if (path == inputDirectory) continue

                if (path.isDirectory())
                {
                    val currentDirFiles = path.tryToResult { it.toFile().listFiles() }
                        .onFailure { onError(this, it) }.get()?.mapNotNull { it.toPath() } ?: continue

                    if (currentDirFiles.isNotEmpty()) continue

                    val deleted = path.deleteIfExists()
                    if (deleted) debug { println("[${this.name}] CleanUp delete empty directory $path") }
                }
                else
                {
                    val hash = path.generateSHA1FromBytes() ?: continue
                    if ((path.absolute() in fileHashes.keys && hash in fileHashes.values) ||
                        (path.absolute() in dirContentHashes.keys && hash in dirContentHashes.values)) continue

                    val deleted = path.deleteIfExists()
                    if (deleted) debug { println("[${this.name}] CleanUp delete file $path") }
                }
            }
        }

        // -- ZIP CREATION --

        outputZipFile.tryToResult { it.createParentDirectories() }.onFailure { onError(this, it) }

        zip(inputDirectory, outputZipFile)

        outputZipFile
    }

    onSuccess(this, timedValue.value, timedValue.duration)
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
                    val action = measureTimedValue {
                        async {
                            packagingAction.action()?.let {
                                onError(it)
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
                                if (error != null) onError(error)
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
                            onError(it)
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
                            if (error != null) onError(error)
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
    }.map { (exportRule, ruleContext) ->
        exportRule.getResult(ruleContext)
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
        rule.getResult(Finished(lockFile, configFile, workingSubDir))
    }

    return results + missing + finished
}
