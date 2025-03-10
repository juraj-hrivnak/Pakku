package teksturepako.pakku.api.actions.sync

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

suspend fun Set<ProjectOverride>.sync(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (projectOverride: ProjectOverride) -> Unit,
    syncPrimaryDirectories: Boolean = false,
): List<Job> = coroutineScope {
    this@sync.mapNotNull { projectOverride ->
        if (projectOverride.isInPrimaryDirectory && !syncPrimaryDirectories) return@mapNotNull null

        launch {
            if (projectOverride.fullOutputPath.exists())
            {
                onError(AlreadyExists(projectOverride.fullOutputPath.toString()))
                return@launch
            }

            projectOverride.fullOutputPath.tryToResult {
                createParentDirectories()
                writeBytes(projectOverride.bytes)
            }.onSuccess {
                onSuccess(projectOverride)
            }.onFailure {
                onError(it)
            }
        }
    }
}
