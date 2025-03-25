package teksturepako.pakku.api.actions.remote

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.debug
import teksturepako.pakku.integration.git.gitUpdate
import teksturepako.pakku.io.FileAction
import kotlin.io.path.exists

suspend fun remoteUpdate(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
    onSync: suspend (FileAction) -> Unit,
    branch: String? = null,
    allowedTypes: Set<OverrideType>?,
): ActionError?
{
    if (LockFile.exists()) return CouldNotInstallRemote()
    if (!Dirs.remoteDir.exists()) return CanNotUpdateRemote()

    debug { println("remoteUpdate") }
    debug { println("starting git & fetching the repo") }

    gitUpdate(Dirs.remoteDir, branch) { taskName, percentDone -> onProgress(taskName, percentDone) }
        ?.onError { return it }

    debug { println("syncing overrides") }

    syncRemoteDirectory(onSync, allowedTypes)
        ?.onError { return it }

    return null
}

class CanNotUpdateRemote: ActionError()
{
    override val rawMessage = "Could not update the remote. No remote was found."
}
