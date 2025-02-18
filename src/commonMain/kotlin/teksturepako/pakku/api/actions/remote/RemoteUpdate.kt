package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.gitHardFetch
import teksturepako.pakku.integration.git.gitStatus
import kotlin.io.path.exists

suspend fun remoteUpdate(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): ActionError?
{
    if (LockFile.exists()) return CanNotInstallRemote()
    if (!Dirs.remoteDir.exists()) return CanNotUpdateRemote()

    val (status, _) = gitStatus(Dirs.remoteDir).getOrElse { return it }

    if (status.isClean) return null

    debug { println("starting git & fetching the repo") }

    gitHardFetch(Dirs.remoteDir) { taskName, percentDone -> onProgress(taskName, percentDone) }
        ?.onError { return it }

    syncRemoteDirectory()

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
