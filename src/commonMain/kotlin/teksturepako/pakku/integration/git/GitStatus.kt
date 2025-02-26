package teksturepako.pakku.integration.git

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.Repository
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.debug
import java.nio.file.Path

data class GitStatusError(val dir: Path, val reason: String? = null): ActionError()
{
    override val rawMessage = message("Failed to get the status of a repository, $reason")
}

suspend fun gitStatus(dir: Path): Result<Pair<Status, Repository>, GitStatusError> = coroutineScope {

    val (git, status, repository) = try
    {
        val git = Git.open(dir.toFile())

        val status = git.status()
            .call()

        Triple(git, status, git.repository)
    }
    catch (e: Exception)
    {
        debug { e.stackTraceToString() }
        return@coroutineScope Err(GitStatusError(dir, e.stackTraceToString()))
    }

    launch { git.close() }.join()

    return@coroutineScope Ok(status to repository)
}
