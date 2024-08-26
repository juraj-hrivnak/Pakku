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
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.createHash
import java.nio.file.Path
import kotlin.io.path.*

fun retrieveProjectFiles(
    lockFile: LockFile,
    platforms: List<Platform>
) : List<Result<ProjectFile, ActionError>> = lockFile.getAllProjects().map { project ->
    val file = platforms.firstNotNullOfOrNull { platform ->
        project.getFilesForPlatform(platform).firstOrNull()
    }

    if (file == null) Err(NoFiles(project, lockFile)) else Ok(file)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun List<ProjectFile>.fetch(
    onError: suspend (error: ActionError) -> Unit,
    onProgress: suspend (advance: Long, total: Long) -> Unit,
    onSuccess: suspend (projectFile: ProjectFile, project: Project?) -> Unit,
    lockFile: LockFile,
) = coroutineScope {
    val maxBytes: AtomicLong = atomic(0L)

    val channel = produce {
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
                    onProgress(bytesSentTotal - prevBytes.value, maxBytes.value)
                    prevBytes.getAndSet(bytesSentTotal)
                }

                if (bytes == null)
                {
                    onError(DownloadFailed(projectFile.getPath()))
                    return@launch
                }

                projectFile.checkIntegrity(bytes).getOrElse {
                    onError(it)
                    return@launch
                }

                send(projectFile to bytes)
            }
        }
    }

    channel.consumeEach { (projectFile, bytes) ->
        launch(Dispatchers.IO) {
            runCatching {
                val file = projectFile.getPath()
                file?.createParentDirectories()
                file?.writeBytes(bytes)
            }.onSuccess {
                onSuccess(projectFile, projectFile.getParentProject(lockFile))
            }.onFailure {
                onError(CouldNotSave(projectFile.getPath(), it.stackTraceToString()))
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
    projectOverrides: Set<ProjectOverride>
) = coroutineScope {
    val projectFileNames = projectFiles.filter { it.hashes?.get("sha1") == null }.map { it.fileName }
    val projectFileHashes = projectFiles.mapNotNull { projectFile -> projectFile.hashes?.get("sha1") }

    val channel = produce {
        ProjectType.entries
            .filterNot { it == ProjectType.WORLD }
            .mapNotNull { projectType ->
                val folder = Path(workingPath, projectType.folderName)
                if (folder.notExists()) return@mapNotNull null
                runCatching { folder.listDirectoryEntries() }.get()
            }.flatten().forEach { file ->
                launch {
                    val bytes = runCatching { file.readBytes() }.get()
                    val fileHash = bytes?.let { createHash("sha1", it) }

                    if (file.extension in listOf("jar", "zip")
                        && fileHash !in projectOverrides.getFileHashes()
                        && file.name !in projectFileNames
                        && fileHash !in projectFileHashes)
                    {
                        send(file)
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