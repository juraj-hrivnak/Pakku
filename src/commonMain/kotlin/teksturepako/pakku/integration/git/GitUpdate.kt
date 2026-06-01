package teksturepako.pakku.integration.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.api.FetchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.debug
import java.nio.file.Path

data class GitUpdateError(val dir: Path, val reason: String? = null): ActionError()
{
    override val rawMessage = message("Failed to fetch a repository, $reason")
}

suspend fun gitUpdate(
    dir: Path,
    branch: String? = null,
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError? = coroutineScope {

    val (progressMonitor, outputStream, writer) = pakkuGitProgressMonitor { taskName, percentDone ->
        onProgress(taskName, percentDone)
    }

    val git = try
    {
        val git = Git.open(dir.toFile())

        if (branch != null)
        {
            git.checkout()
                .setProgressMonitorIfPossible(progressMonitor)
                .setName(branch)
                .call()
        }

        git.clean()
            .setForce(true)
            .call()

        git.fetch()
            .setProgressMonitorIfPossible(progressMonitor)
            .call()

        git.reset()
            .setProgressMonitorIfPossible(progressMonitor)
            .setMode(ResetCommand.ResetType.HARD)
            .setRef(git.repository.getRemoteName(git.repository.branch))
            .call()

        git
    }
    catch (e: Exception)
    {
        debug { e.printStackTrace() }
        return@coroutineScope GitUpdateError(dir)
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

suspend fun gitFetchCheckout(
    dir: Path,
    ref: String,
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError? = coroutineScope {

    val (progressMonitor, outputStream, writer) = pakkuGitProgressMonitor { taskName, percentDone ->
        onProgress(taskName, percentDone)
    }

    val git = try
    {
        val git = Git.open(dir.toFile())

        git.clean()
            .setForce(true)
            .call()

        git.fetch()
            .setProgressMonitorIfPossible(progressMonitor)
            .call()

        git.checkout()
            .setProgressMonitorIfPossible(progressMonitor)
            .setName(ref)
            .call()

        git
    }
    catch (e: Exception)
    {
        debug { e.printStackTrace() }
        return@coroutineScope GitUpdateError(dir)
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

private fun CheckoutCommand.setProgressMonitorIfPossible(progressMonitor: ProgressMonitor?): CheckoutCommand =
    if (progressMonitor == null) this else this.setProgressMonitor(progressMonitor)

private fun FetchCommand.setProgressMonitorIfPossible(progressMonitor: ProgressMonitor?): FetchCommand =
    if (progressMonitor == null) this else this.setProgressMonitor(progressMonitor)

private fun ResetCommand.setProgressMonitorIfPossible(progressMonitor: ProgressMonitor?): ResetCommand =
    if (progressMonitor == null) this else this.setProgressMonitor(progressMonitor)

fun gitRemoteUrl(dir: Path, remoteName: String = "origin"): String? =
    runCatching { Git.open(dir.toFile()).use { git -> git.repository.config.getString("remote", remoteName, "url") } }
        .getOrNull()

fun gitHeadCommit(dir: Path): String =
    Git.open(dir.toFile()).use { git ->
        git.repository.resolve(Constants.HEAD)?.name ?: error("HEAD commit not found in repository: $dir")
    }

fun gitIsClean(dir: Path): Boolean =
    Git.open(dir.toFile()).use { git -> git.status().call().isClean }

fun gitSetRemoteUrl(dir: Path, remoteName: String = "origin", url: String)
{
    Git.open(dir.toFile()).use { git ->
        val config = git.repository.config
        config.setString("remote", remoteName, "url", url)
        config.save()
    }
}

fun gitRefType(dir: Path, ref: String): ConfigFile.RefType =
    Git.open(dir.toFile()).use { git ->
        val repository: Repository = git.repository
        when
        {
            repository.findRef("refs/heads/$ref") != null || repository.findRef("refs/remotes/origin/$ref") != null -> ConfigFile.RefType.BRANCH
            repository.findRef("refs/tags/$ref") != null -> ConfigFile.RefType.TAG
            repository.resolve(ref) != null -> ConfigFile.RefType.COMMIT
            else -> ConfigFile.RefType.BRANCH
        }
    }
