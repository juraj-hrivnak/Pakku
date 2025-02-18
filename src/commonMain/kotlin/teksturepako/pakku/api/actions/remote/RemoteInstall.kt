package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.hint
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.gitClone
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

suspend fun remoteInstall(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
    onSync: suspend (Result<Pair<Path, Path>, ActionError>) -> Unit,
    remoteUrl: String,
    branch: String? = null,
): ActionError?
{
    if (LockFile.exists()) return CanNotInstallRemote(remoteUrl)
    if (Dirs.remoteDir.exists()) return RemoteAlreadyExists(remoteUrl)

    return when
    {
        remoteUrl.endsWith(".git") || remoteUrl.contains("github.com") ->
        {
            handleGit(
                remoteUrl, branch,
                onProgress = { taskName, percentDone -> onProgress(taskName, percentDone) },
                onSync = { result -> onSync(result) },
            )
        }
        else -> InvalidUrl(remoteUrl)
    }
}

data class RemoteAlreadyExists(val url: String): ActionError()
{
    override val rawMessage = message(
        "Can not install remote: '$url'.",
        "A remote for this modpack already exists.",
        hint("use \"pakku remote rm\" to remove the remote from your modpack"),
        hint("use \"pakku remote update\" to update the remote"),
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
    onSync: suspend (Result<Pair<Path, Path>, ActionError>) -> Unit
): ActionError?
{
    debug { println("starting git & cloning the repo") }

    gitClone(uri, Dirs.remoteDir, branch) { taskName, percentDone -> onProgress(taskName, percentDone) }
        ?.onError {
            remoteRemove()
            return it
        }

    if (!LockFile.existsAt(Path(Dirs.remoteDir.pathString, LockFile.FILE_NAME)))
    {
        remoteRemove()
        return FileNotFound(Path(Dirs.remoteDir.pathString, LockFile.FILE_NAME).pathString)
    }

    syncRemoteDirectory { result -> onSync(result) }
        ?.onError { return it }

    return null
}
