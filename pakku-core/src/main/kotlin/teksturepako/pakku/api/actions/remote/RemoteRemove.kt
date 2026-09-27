package teksturepako.pakku.api.actions.remote

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.io.tryToResult
import java.nio.file.Path

class FailedToRemoveRemote : ActionError()
{
    override val rawMessage = "Failed to remove the remote."
}

suspend fun remoteRemove(): Result<Path, ActionError> = Dirs.remoteDir.tryToResult { toFile().deleteRecursively() }
    .map { if (!it) return Err(FailedToRemoveRemote()) else Dirs.remoteDir }
