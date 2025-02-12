package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.copyOverride
import teksturepako.pakku.api.overrides.copyProjectOverrides
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.debug
import teksturepako.pakku.io.tryToResult
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.pathString

suspend fun remoteInstall(remoteUrl: String)
{
    if (!remoteUrl.startsWith("https://")) return

    when
    {
        remoteUrl.endsWith(".git") || remoteUrl.contains("github.com") ->
        {
            installFromGit(remoteUrl)
        }
    }
}

suspend fun installFromGit(uri: String) = coroutineScope {

    val localPath = Path(workingPath, Dirs.PAKKU_DIR, "remote")

    if (localPath.exists()) localPath.tryToResult { toFile().deleteRecursively() }

    val git = try
    {
        Git
            .cloneRepository()
            .setURI(uri)
            .setDirectory(localPath.toFile())
            .setDepth(1)
            .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
            .call()
    }
    catch (e: Exception)
    {
        println(e.stackTraceToString())
        return@coroutineScope
    }

    val configFile: ConfigFile? = if (ConfigFile.existsAt(localPath))
    {
        ConfigFile.readToResultFrom(localPath).getOrElse {
            return@getOrElse null
        } ?: return@coroutineScope
    }
    else null

    // -- OVERRIDES --

    debug { println("copping overrides") }

    if (configFile != null)
    {
        val overrides = getOverridesAsyncFrom(localPath, configFile).awaitAllAndGet()

        for ((overridePath, _) in overrides)
        {
            val inputPath = Path(localPath.pathString, overridePath)
            val outputPath = Path(workingPath, overridePath)

            copyOverride(inputPath, outputPath)
        }
    }

    // -- PROJECT OVERRIDES --

    debug { println("copping project overrides") }

    copyProjectOverrides(localPath, Path(workingPath))

    // -- LOCK FILE & CONFIG FILE --

    debug { println("copping config file & lock file") }

    Path(localPath.pathString, LockFile.FILE_NAME).tryToResult {
        copyTo(Path(workingPath, LockFile.FILE_NAME), overwrite = true)
    }

    Path(localPath.pathString, ConfigFile.FILE_NAME).tryToResult {
        copyTo(Path(workingPath, ConfigFile.FILE_NAME), overwrite = true)
    }

    // -- CLEAN UP --

    debug { println("closing git") }

    launch { git.close() }.join()

    debug { println("cleaning up") }
    localPath.tryToResult { toFile().deleteRecursively() }
}
