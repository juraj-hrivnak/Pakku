package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.*
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.http.requestByteArray
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectFile
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

fun retrieveProjectFiles(
    lockFile: LockFile,
    providers: List<Provider>
) : List<Result<ProjectFile, ActionError>> = lockFile.getAllProjects().map { project ->
    val file = project.getLatestFile(providers)

    if (file == null) Err(NoFiles(project, lockFile)) else Ok(file)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun List<ProjectFile>.fetch(
    onError: suspend (error: ActionError) -> Unit,
    onProgress: suspend (completed: Long, total: Long) -> Unit,
    onSuccess: suspend (path: Path, projectFile: ProjectFile) -> Unit,
    lockFile: LockFile, configFile: ConfigFile?, retry: Int? = null
) = coroutineScope {
    tailrec suspend fun tryFetch(projectFiles: List<ProjectFile>, retryNumber: Int = 0)
    {
        val totalBytes: AtomicLong = atomic(0L)
        val completedBytes: AtomicLong = atomic(0L)
        
        val fetchChannel = produce {
            for (projectFile in projectFiles)
            {
                launch {
                    val parentProject = projectFile.getParentProject(lockFile) ?: return@launch

                    val path = projectFile.getPath(parentProject, configFile)

                    if (path.exists())
                    {
                        onError(AlreadyExists(path.toString()))
                        return@launch
                    }

                    totalBytes += projectFile.size.toLong()
                    val prevBytes: AtomicLong = atomic(0L)

                    val bytes = requestByteArray(projectFile.url!!) { bytesSentTotal, _ ->
                        completedBytes.getAndAdd(bytesSentTotal - prevBytes.value)

                        onProgress(completedBytes.value, totalBytes.value)
                        prevBytes.getAndSet(bytesSentTotal)
                    }.get()

                    if (bytes == null)
                    {
                        onError(DownloadFailed(path, retryNumber))
                        send(Err(projectFile))
                        return@launch
                    }

                    projectFile.checkIntegrity(bytes, path)?.let { err ->
                        onError(err)

                        if (err is HashMismatch) return@launch
                    }

                    send(Ok(Triple(path, projectFile, bytes)))
                }
            }
        }

        val jobs = mutableListOf<Job>()
        val fails = mutableListOf<Deferred<ProjectFile>>()

        fetchChannel.consumeEach { result ->
            result.onSuccess { (path, projectFile, bytes) ->
                jobs += launch(Dispatchers.IO) {
                    runCatching {
                        path.createParentDirectories()
                        path.writeBytes(bytes)
                    }.onSuccess {
                        onSuccess(path, projectFile)
                    }.onFailure {
                        onError(CouldNotSave(path, it.stackTraceToString()))
                    }
                }
            }.onFailure { projectFile ->
                fails += async {
                    projectFile
                }
            }
        }

        jobs.joinAll()

        val filesToRetry = fails.awaitAll()

        if (retry != null && retryNumber < retry && retryNumber < 3 && filesToRetry.isNotEmpty())
        {
            tryFetch(filesToRetry, retryNumber + 1)
        }
    }

    launch {
        tryFetch(this@fetch)
    }
}
