package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.copyOverride
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.debug
import kotlin.io.path.Path
import kotlin.io.path.pathString

suspend fun syncRemoteDirectory() = coroutineScope {

    debug { println("reading config file") }

    val configFile: ConfigFile? = if (ConfigFile.existsAt(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)))
    {
        ConfigFile.readToResultFrom(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)).getOrElse { error ->
            return@coroutineScope error
        }
    }
    else null

    return@coroutineScope launch(Dispatchers.IO) {
        debug { println("copping overrides") }

        if (configFile != null)
        {
            val overrides = getOverridesAsyncFrom(Dirs.remoteDir, configFile).awaitAllAndGet()

            for ((overridePath, _) in overrides)
            {
                val inputPath = Path(Dirs.remoteDir.pathString, overridePath)
                val outputPath = Path(workingPath, overridePath)

                copyOverride(inputPath, outputPath)
            }
        }
    }
}