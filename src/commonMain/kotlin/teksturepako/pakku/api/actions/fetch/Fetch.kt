package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.*
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

fun retrieveProjectFiles(
    lockFile: LockFile,
    platforms: List<Platform>
) : List<Result<ProjectFile, ActionError>> = lockFile.getAllProjects().map { project ->
    val file = platforms.firstNotNullOfOrNull { platform ->
        project.getFilesForPlatform(platform).firstOrNull()
    }

    if (file == null)
    {
        Err(NoFiles(project, lockFile))
    }
    else Ok(file)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun List<ProjectFile>.fetch(
    onError: suspend (error: ActionError) -> Unit,
    onProgress: suspend (advance: Long, total: Long, sent: Long) -> Unit,
    onSuccess: suspend (projectFile: ProjectFile, project: Project?) -> Unit,
    lockFile: LockFile,
) = coroutineScope {
    val maxBytes: AtomicLong = atomic(0L)

    produce {
        for (projectFile in this@fetch)
        {
            launch {
                if (projectFile.getPath(lockFile)?.exists() == true)
                {
                    onError(AlreadyExists(projectFile.getPath().toString()))
                    return@launch
                }

                maxBytes += projectFile.size.toLong()
                val prevBytes: AtomicLong = atomic(0L)

                val bytes = Http().requestByteArray(projectFile.url!!) { bytesSentTotal, _ ->
                    onProgress(bytesSentTotal - prevBytes.value, maxBytes.value, bytesSentTotal)
                    prevBytes.getAndSet(bytesSentTotal)
                }

                if (bytes == null)
                {
                    onError(DownloadFailed(projectFile))
                    return@launch
                }

                projectFile.checkIntegrity(bytes).getOrElse {
                    onError(it)
                    return@launch
                }

                send(projectFile to bytes)
            }
        }
    }.consumeEach { (projectFile, bytes) ->
        launch(Dispatchers.IO) {
            runCatching {
                val file = projectFile.getPath()
                file?.createParentDirectories()
                file?.writeBytes(bytes)
            }.onSuccess {
                onSuccess(projectFile, projectFile.getParentProject(lockFile))
            }.onFailure {
                onError(CouldNotSave(projectFile, it.stackTraceToString()))
            }
        }
    }
}