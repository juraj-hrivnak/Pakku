package teksturepako.pakku.integration.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.debug
import java.nio.file.Path

data class GitCloneError(val uri: String, val reason: String? = null) : ActionError()
{
    override val rawMessage = message("Failed to clone a repository from: '$uri'", reason)
}

suspend fun gitClone(
    uri: String,
    dir: Path,
    branch: String? = null,
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError? = coroutineScope {

    val (progressMonitor, outputStream, writer) = pakkuGitProgressMonitor { taskName, percentDone ->
        onProgress(taskName, percentDone)
    }

    val git = try
    {
        Git
            .cloneRepository()
            .setURI(uri)
            .setBranchIfPossible(branch)
            .setDirectory(dir.toFile())
            .setDepth(1)
            .setProgressMonitorIfPossible(progressMonitor)
            .call()
    }
    catch (e: Exception)
    {
        debug { e.stackTraceToString() }
        return@coroutineScope GitCloneError(uri)
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

private fun CloneCommand.setBranchIfPossible(branch: String?): CloneCommand =
    if (branch == null) this else this.setBranch(branch)

private fun CloneCommand.setProgressMonitorIfPossible(progressMonitor: ProgressMonitor?): CloneCommand =
    if (progressMonitor == null) this else this.setProgressMonitor(progressMonitor)
