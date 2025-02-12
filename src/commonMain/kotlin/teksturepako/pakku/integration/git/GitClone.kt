package teksturepako.pakku.integration.git

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.debug
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration

data class GitProgress(
    val taskName: String?,
    val percentDone: Int,
)

data class GitCloneError(val uri: String, val reason: String? = null): ActionError()
{
    override val rawMessage = message("Failed to clone a repository from: '$uri'", reason)
}

suspend fun gitClone(
    uri: String,
    dir: Path,
    branch: String? = null,
    onProgress: (GitProgress) -> Unit,
): Result<Git, ActionError>
{
    val stream = ByteArrayOutputStream()
    val writer = BufferedWriter(OutputStreamWriter(stream, StandardCharsets.UTF_8))

    val progressMonitor = object : TextProgressMonitor(writer)
    {
        override fun onUpdate(taskName: String?, workCurr: Int, workTotal: Int, percentDone: Int, duration: Duration?)
        {
            super.onUpdate(taskName, workCurr, workTotal, percentDone, duration)

            runCatching { writer.flush() }

            val progress = GitProgress(
                taskName = taskName,
                percentDone = percentDone,
            )

            onProgress(progress)
            stream.reset()
        }
    }

    val git = try
    {
        Git
            .cloneRepository()
            .setURI(uri)
            .setBranchIfPossible(branch)
            .setDirectory(dir.toFile())
            .setDepth(1)
            .setProgressMonitor(progressMonitor)
            .call()
    }
    catch (e: Exception)
    {
        debug { e.stackTraceToString() }
        return Err(GitCloneError(uri))
    }
    finally
    {
        withContext(Dispatchers.IO) {
            writer.close()
            stream.close()
        }
    }

    return Ok(git)
}

private fun CloneCommand.setBranchIfPossible(branch: String?): CloneCommand =
    if (branch == null) this else this.setBranch(branch)
