package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.copyOverride
import teksturepako.pakku.api.overrides.copyProjectOverrideDirectories
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.GitProgress
import teksturepako.pakku.integration.git.gitClone
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.pathString

suspend fun remoteInstall(
    remoteUrl: String,
    branch: String? = null,
    onProgress: (GitProgress) -> Unit,
): ActionError?
{
    if (!remoteUrl.startsWith("https://")) return InvalidUrl(remoteUrl)

    return when
    {
        remoteUrl.endsWith(".git") || remoteUrl.contains("github.com") ->
        {
            installFromGit(remoteUrl, branch) { onProgress(it) }
        }
        else -> InvalidUrl(remoteUrl)
    }
}

data class InvalidUrl(val url: String): ActionError()
{
    override val rawMessage = "Invalid URL: '$url'"
}

suspend fun installFromGit(
    uri: String,
    branch: String?,
    onProgress: (GitProgress) -> Unit,
): ActionError? = coroutineScope {

    val localPath = Path(workingPath, Dirs.PAKKU_DIR, "remote")

    if (localPath.exists()) localPath.tryToResult { toFile().deleteRecursively() }

    debug { println("starting git") }

    val git = gitClone(uri, localPath, branch) { onProgress(it) }.getOrElse { error ->
        return@coroutineScope error
    }

    val configFile: ConfigFile? = if (ConfigFile.existsAt(localPath))
    {
        ConfigFile.readToResultFrom(localPath).getOrElse { error ->
            return@coroutineScope error
        }
    }
    else null

    val jobs = mutableListOf<Job>()

    // -- OVERRIDES --

    jobs += launch {
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
    }

    // -- PROJECT OVERRIDES --

    jobs += launch {
        debug { println("copping project overrides") }

        copyProjectOverrideDirectories(localPath, Path(workingPath))
    }

    // -- LOCK FILE & CONFIG FILE --

    jobs += launch {
        debug { println("copping config file & lock file") }

        Path(localPath.pathString, LockFile.FILE_NAME).tryToResult {
            copyTo(Path(workingPath, LockFile.FILE_NAME), overwrite = true)
        }

        Path(localPath.pathString, ConfigFile.FILE_NAME).tryToResult {
            copyTo(Path(workingPath, ConfigFile.FILE_NAME), overwrite = true)
        }
    }

    // -- CLEAN UP --

    jobs.joinAll()

    debug { println("closing git") }

    launch { git.close() }.join()

    debug { println("cleaning up") }
    localPath.tryToResult { toFile().deleteRecursively() }

    return@coroutineScope null
}
