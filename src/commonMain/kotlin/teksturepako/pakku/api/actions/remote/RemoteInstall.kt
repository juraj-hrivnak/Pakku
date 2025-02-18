package teksturepako.pakku.api.actions.remote

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.gitClone
import kotlin.io.path.exists

suspend fun remoteInstall(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
    remoteUrl: String,
    branch: String? = null,
): ActionError?
{
    return when
    {
        remoteUrl.endsWith(".git") || remoteUrl.contains("github.com") ->
        {
            handleGit(remoteUrl, branch) { taskName, percentDone -> onProgress(taskName, percentDone) }
        }
        else -> InvalidUrl(remoteUrl)
    }
}

data class CanNotInstallRemote(val url: String): ActionError()
{
    override val rawMessage = message(
        "Can not install remote: '$url'",
        "Only one remote can be installed at once.",
        newline = true,
    )
}

data class InvalidUrl(val url: String): ActionError()
{
    override val rawMessage = "Invalid URL: '$url'"
}

private suspend fun handleGit(
    uri: String,
    branch: String?,
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError?
{
    if (Dirs.remoteDir.exists()) return CanNotInstallRemote(uri)

    debug { println("starting git & cloning the repo") }

    gitClone(uri, Dirs.remoteDir, branch) { taskName, percentDone -> onProgress(taskName, percentDone) }
        ?.onError { return it }

    syncRemoteDirectory()

    return null
}

