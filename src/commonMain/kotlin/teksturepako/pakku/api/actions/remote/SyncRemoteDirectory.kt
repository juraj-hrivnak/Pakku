package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.copyOverride
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.debug
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

suspend fun syncRemoteDirectory(
    onSync: suspend (Result<Pair<Path, Path>, ActionError>) -> Unit
): ActionError? = coroutineScope {

    debug { println("reading config file") }

    val configFile: ConfigFile = if (ConfigFile.existsAt(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)))
    {
        ConfigFile.readToResultFrom(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)).getOrElse { error ->
            return@coroutineScope error
        }
    }
    else return@coroutineScope null

    debug { println("copping overrides") }

    val overrides = getOverridesAsyncFrom(Dirs.remoteDir, configFile).awaitAll()

    overrides.map { (overridePath, _) ->
        launch(Dispatchers.IO) {
            val inputPath = Path(Dirs.remoteDir.pathString, overridePath)
            val outputPath = Path(workingPath, overridePath)

            val result = copyOverride(inputPath, outputPath, cleanUpDirectory = true)

            onSync(result)
        }
    }.joinAll()

    return@coroutineScope null
}