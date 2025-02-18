package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.gitHardFetch
import teksturepako.pakku.integration.git.gitStatus
import java.nio.file.Path
import kotlin.io.path.exists

suspend fun remoteUpdate(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
    onSync: suspend (Result<Pair<Path, Path>, ActionError>) -> Unit,
): ActionError?
{
    if (LockFile.exists()) return CanNotInstallRemote()
    if (!Dirs.remoteDir.exists()) return CanNotUpdateRemote()

    val (status, _) = gitStatus(Dirs.remoteDir).getOrElse { return it }

    if (!status.isClean)
    {
        debug { println("starting git & fetching the repo") }

        gitHardFetch(Dirs.remoteDir) { taskName, percentDone -> onProgress(taskName, percentDone) }
            ?.onError { return it }
    }

    debug { println("syncing overrides") }

    syncRemoteDirectory { result -> onSync(result) }
        ?.onError { return it }

    return null
}

class CanNotUpdateRemote: ActionError()
{
    override val rawMessage = message(
        "Can not update remote.",
        "No remote was found.",
        newline = true,
    )
}
