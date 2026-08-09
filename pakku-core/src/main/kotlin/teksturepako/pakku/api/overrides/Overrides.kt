package teksturepako.pakku.api.overrides

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.workingPath
import java.nio.file.Path

suspend fun getOverridesAsync(configFile: ConfigFile): OverridesDeferred =
    getOverridesAsyncFrom(Path.of(workingPath), configFile)

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

    return@coroutineScope OverridesDeferred(path, overrides, serverOverrides, clientOverrides)
}

data class OverridesDeferred(
    val root: Path,
    val overrides: Deferred<List<Result<String, ActionError>>>,
    val serverOverrides: Deferred<List<Result<String, ActionError>>>,
    val clientOverrides: Deferred<List<Result<String, ActionError>>>,
)
{
    suspend fun awaitAll(): List<OverrideSource>
    {
        val results = listOf(
            overrides.await() to OverrideType.OVERRIDE,
            serverOverrides.await() to OverrideType.SERVER_OVERRIDE,
            clientOverrides.await() to OverrideType.CLIENT_OVERRIDE
        )

        return results.flatMap {
            it.first.mapNotNull { result ->
                val pathString = result.get() ?: return@mapNotNull null
                OverrideSource(root, pathString, it.second)
            }
        }
    }
}

data class OverrideSource(val root: Path, val path: String, val type: OverrideType)
