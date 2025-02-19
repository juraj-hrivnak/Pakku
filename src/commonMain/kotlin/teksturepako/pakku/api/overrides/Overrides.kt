package teksturepako.pakku.api.overrides

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import java.nio.file.Path

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
                val pathString = result.get() ?: return@mapNotNull null
                pathString to it.second
            }
        }
    }
}
