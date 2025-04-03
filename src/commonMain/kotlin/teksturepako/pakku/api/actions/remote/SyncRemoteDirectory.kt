package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.toMultipleErrors
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.debug
import teksturepako.pakku.io.FileAction
import teksturepako.pakku.io.copyRecursivelyTo
import kotlin.io.path.Path
import kotlin.io.path.pathString

suspend fun syncRemoteDirectory(
    onSync: suspend (FileAction) -> Unit,
    allowedTypes: Set<OverrideType>?,
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

    val error = overrides.map { (overridePath, overrideType) ->
        async(Dispatchers.IO) {
            if (allowedTypes != null && overrideType !in allowedTypes) return@async null

            val inputPath = Path(Dirs.remoteDir.pathString, overridePath)
            val outputPath = Path(workingPath, overridePath)

            inputPath.copyRecursivelyTo(outputPath) { onSync(it) }
        }
    }.awaitAll().toMultipleErrors()

    return@coroutineScope error
}
