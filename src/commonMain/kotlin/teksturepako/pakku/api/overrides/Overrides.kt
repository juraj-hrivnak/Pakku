package teksturepako.pakku.api.overrides

import com.github.michaelbull.result.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.errors.CouldNotSave
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.debug
import teksturepako.pakku.io.cleanUpDirectoryRelative
import teksturepako.pakku.io.tryToResult
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

suspend fun getOverridesAsync(configFile: ConfigFile): OverridesDeferred = coroutineScope {
    val overrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllOverrides()
    }

    val serverOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllServerOverrides()
    }

    val clientOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllClientOverrides()
    }

    return@coroutineScope OverridesDeferred(overrides, serverOverrides, clientOverrides)
}

suspend fun getOverridesAsyncFrom(path: Path, configFile: ConfigFile): OverridesDeferred = coroutineScope {
    val overrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllOverridesFrom(path)
    }

    val serverOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllServerOverridesFrom(path)
    }

    val clientOverrides: Deferred<List<Result<String, ActionError>>> = async {
        configFile.getAllClientOverridesFrom(path)
    }

    return@coroutineScope OverridesDeferred(overrides, serverOverrides, clientOverrides)
}

data class OverridesDeferred(
    val overrides: Deferred<List<Result<String, ActionError>>>,
    val serverOverrides: Deferred<List<Result<String, ActionError>>>,
    val clientOverrides: Deferred<List<Result<String, ActionError>>>,
)
{
    suspend fun awaitAll(): List<Pair<String, OverrideType>>
    {
        val results = listOf(
            overrides.await() to OverrideType.OVERRIDE,
            serverOverrides.await() to OverrideType.SERVER_OVERRIDE,
            clientOverrides.await() to OverrideType.CLIENT_OVERRIDE
        )

        return results.flatMap {
            it.first.mapNotNull { result ->
                val path = result.get() ?: return@mapNotNull null
                path to it.second
            }
        }
    }
}

suspend fun copyOverride(
    inputPath: Path, outputPath: Path,
    cleanUpDirectory: Boolean = false
): Result<Pair<Path, Path>, ActionError>
{
    return when
    {
        inputPath.isRegularFile() ->
        {
            outputPath.tryToResult { createParentDirectories() }
                .onFailure { error ->
                    if (error !is AlreadyExists) return Err(error)
                }

            inputPath.tryToResult { copyTo(outputPath, overwrite = true) }
                .fold(
                    success = { Ok(inputPath to outputPath) },
                    failure = { Err(it) }
                )
        }

        inputPath.isDirectory()   ->
        {
            outputPath.tryToResult { createParentDirectories() }
                .onFailure { error ->
                    if (error !is AlreadyExists) return Err(error)
                }

            val result = inputPath.tryToResult { toFile().copyRecursively(outputPath.toFile(), overwrite = true) }
                .fold(
                    success = { Ok(inputPath to outputPath) },
                    failure = { Err(it) }
                )

            if (cleanUpDirectory)
            {
                cleanUpDirectoryRelative(
                    outputPath, listOf(inputPath),
                    onError = { debug { println(it.rawMessage) } },
                    onAction = { debug { println(it) } },
                )
            }

            result
        }

        else                      ->
        {
            Err(CouldNotSave(outputPath))
        }
    }
}
