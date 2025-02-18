package teksturepako.pakku.integration.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.debug
import java.nio.file.Path

data class GitHardFetchError(val dir: Path, val reason: String? = null): ActionError()
{
    override val rawMessage = message("Failed to hard fetch a repository, $reason")
}

suspend fun gitHardFetch(
    dir: Path,
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError? = coroutineScope {

    val (progressMonitor, outputStream, writer) = pakkuGitProgressMonitor { taskName, percentDone ->
        onProgress(taskName, percentDone)
    }

    val git = try
    {
        val git = Git.open(dir.toFile())

        git.fetch()
            .setProgressMonitor(progressMonitor)
            .call()

        git.reset()
            .setProgressMonitor(progressMonitor)
            .setMode(ResetCommand.ResetType.HARD)
            .setRef("origin/${git.repository.branch}")
            .call()

        git
    }
    catch (e: Exception)
    {
        debug { e.stackTraceToString() }
        return@coroutineScope GitHardFetchError(dir, e.stackTraceToString())
    }
    finally
    {
        withContext(Dispatchers.IO) {
            writer.close()
            outputStream.close()
        }
    }

    launch { git.close() }.join()

    return@coroutineScope null
}