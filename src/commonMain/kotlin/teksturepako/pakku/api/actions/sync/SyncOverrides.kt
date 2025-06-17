package teksturepako.pakku.api.actions.sync

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.overrides.ManualOverride
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

suspend fun Set<ManualOverride>.sync(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (manualOverride: ManualOverride) -> Unit,
    syncPrimaryDirectories: Boolean = false,
): List<Job> = coroutineScope {
    this@sync.mapNotNull { manualOverride ->
        if (manualOverride.isInPrimaryDirectory && !syncPrimaryDirectories) return@mapNotNull null

        launch {
            if (manualOverride.fullOutputPath.exists())
            {
                onError(AlreadyExists(manualOverride.fullOutputPath.toString()))
                return@launch
            }

            manualOverride.fullOutputPath.tryOrNull {
                createParentDirectories()
            }

            manualOverride.path.tryToResult {
                copyTo(manualOverride.fullOutputPath)
            }.onSuccess {
                onSuccess(manualOverride)
            }.onFailure {
                onError(it)
            }
        }
    }
}
