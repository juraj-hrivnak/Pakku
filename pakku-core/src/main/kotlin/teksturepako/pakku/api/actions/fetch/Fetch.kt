package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.*
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.requestByteArray
import teksturepako.pakku.api.http.requireHttpsWhenUnverifiable
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.io.IllegalPath
import teksturepako.pakku.io.isWithinBounds
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

fun retrieveProjectFiles(
    lockFile: LockFile,
    providers: List<Provider>,
    allowedTypes: Set<OverrideType>? = null,
) : List<Result<ProjectFile, ActionError>> = lockFile.getAllProjects().mapNotNull { project ->
    if (allowedTypes != null && OverrideType.fromProject(project) !in allowedTypes) return@mapNotNull null

    val file = project.getLatestFile(providers)

    if (file == null) Err(NoFiles(project, lockFile)) else Ok(file)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun List<ProjectFile>.fetch(
    onError: suspend (error: ActionError) -> Unit,
    onProgress: suspend (completed: Long, total: Long) -> Unit,
    onSuccess: suspend (path: Path, projectFile: ProjectFile) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile?,
    retry: Int? = null,
    outputDir: Path = Path(workingPath)
) = coroutineScope {
    tailrec suspend fun tryFetch(projectFiles: List<ProjectFile>, retryNumber: Int = 0)
    {
        val totalBytes: AtomicLong = atomic(0L)
        val completedBytes: AtomicLong = atomic(0L)
        
        val fetchChannel = this@coroutineScope.produce {
            for (projectFile in projectFiles)
            {
                launch {
                    val parentProject = projectFile.getParentProject(lockFile) ?: return@launch

                    val path = projectFile.getPath(parentProject, configFile, outputDir)
                    if (path == null || !path.isWithinBounds(outputDir))
                    {
                        onError(IllegalPath(projectFile.fileName))
                        return@launch
                    }

                    if (path.exists())
                    {
                        onError(AlreadyExists(path.toString()))
                        return@launch
                    }

                    val url = projectFile.url
                    if (url == null)
                    {
                        onError(NoUrl(projectFile))
                        return@launch
                    }

                    requireHttpsWhenUnverifiable(url, projectFile.hashes)?.let { error ->
                        onError(error)
                        return@launch
                    }

                    totalBytes += projectFile.size.toLong()
                    val prevBytes: AtomicLong = atomic(0L)

                    val bytes = requestByteArray(url) { bytesSentTotal, _ ->
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
            result
                .onOk { (path, projectFile, bytes) ->
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
                }.onErr { projectFile ->
                    fails += this@coroutineScope.async {
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
