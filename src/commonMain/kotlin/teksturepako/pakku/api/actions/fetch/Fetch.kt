package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.*
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.actions.sync.getFileHashes
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.createHash
import java.nio.file.Path
import kotlin.io.path.*

fun retrieveProjectFiles(
    lockFile: LockFile,
    providers: List<Provider>
) : List<Result<ProjectFile, ActionError>> = lockFile.getAllProjects().map { project ->
    val file = providers.firstNotNullOfOrNull { provider ->
        project.getFilesForProvider(provider).firstOrNull()
    }

    if (file == null) Err(NoFiles(project, lockFile)) else Ok(file)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun List<ProjectFile>.fetch(
    onError: suspend (error: ActionError) -> Unit,
    onProgress: (advance: Long, total: Long) -> Unit,
    onSuccess: suspend (path: Path, projectFile: ProjectFile) -> Unit,
    lockFile: LockFile, configFile: ConfigFile?
) = coroutineScope {
    val maxBytes: AtomicLong = atomic(0L)

    val channel = produce {
        for (projectFile in this@fetch)
        {
            launch {
                val parentProject = projectFile.getParentProject(lockFile)?: return@launch

                val path = projectFile.getPath(parentProject, configFile)

                if (path.exists())
                {
                    onError(AlreadyExists(path.toString()))
                    return@launch
                }

                maxBytes += projectFile.size.toLong()
                val prevBytes: AtomicLong = atomic(0L)

                val bytes = Http().requestByteArray(projectFile.url!!) { bytesSentTotal, _ ->
                    onProgress(bytesSentTotal - prevBytes.value, maxBytes.value)
                    prevBytes.getAndSet(bytesSentTotal)
                }

                if (bytes == null)
                {
                    onError(DownloadFailed(path))
                    return@launch
                }

                projectFile.checkIntegrity(bytes, path)?.let { err ->
                    onError(err)

                    if (err is HashMismatch) return@launch
                }

                send(Triple(path, projectFile, bytes))
            }
        }
    }

    channel.consumeEach { (path, projectFile, bytes) ->
        launch(Dispatchers.IO) {
            runCatching {
                path.createParentDirectories()
                path.writeBytes(bytes)
            }.onSuccess {
                onSuccess(path, projectFile)
            }.onFailure {
                onError(CouldNotSave(path, it.stackTraceToString()))
            }
        }
    }

    launch {
        if (channel.isEmpty) this.cancel()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun deleteOldFiles(
    onSuccess: suspend (file: Path) -> Unit,
    projectFiles: List<ProjectFile>,
    projectOverrides: Set<ProjectOverride>,
    configFile: ConfigFile?
) = coroutineScope {
    val projectFileNames = projectFiles.filter { it.hashes?.get("sha1") == null }.map { it.fileName }
    val projectFileHashes = projectFiles.mapNotNull { projectFile -> projectFile.hashes?.get("sha1") }

    val channel = produce {
        ProjectType.entries
            .filterNot { it == ProjectType.WORLD }
            .mapNotNull { projectType ->
                val folder = Path(workingPath, projectType.getPathString(configFile))
                if (folder.notExists()) return@mapNotNull null
                runCatching { folder.listDirectoryEntries() }.get()
            }
            .flatten()
            .forEach { path ->
                launch {
                    val pathBytes = runCatching { path.readBytes() }.get()
                    val pathHash = pathBytes?.let { createHash("sha1", it) }

                    if (path.extension in listOf("jar", "zip")
                        && pathHash !in projectOverrides.getFileHashes()
                        && path.name !in projectFileNames
                        && pathHash !in projectFileHashes)
                    {
                        send(path)
                    }
                }
            }
    }

    channel.consumeEach { file ->
        launch(Dispatchers.IO) {
            val deleted = file.deleteIfExists()
            if (deleted) onSuccess(file)
        }
    }

    launch {
        if (channel.isEmpty) this.cancel()
    }
}