package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.*
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

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
    onProgress: suspend (advance: Long, total: Long) -> Unit,
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

    val jobs = mutableListOf<Job>()

    channel.consumeEach { (path, projectFile, bytes) ->
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
    }

    jobs.joinAll()
    this.cancel()
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun deleteOldFiles(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (file: Path) -> Unit,
    projectFiles: List<ProjectFile>,
    projectOverrides: Set<ProjectOverride>,
    lockFile: LockFile,
    configFile: ConfigFile?
) = coroutineScope {

    val fileHashes = async { projectFiles
        .map { projectFile ->
            async x@ {
                val parentProject = projectFile.getParentProject(lockFile) ?: return@x null

                val path = projectFile.getPath(parentProject, configFile)

                path.tryToResult { it.readBytes() }
                    .onFailure { onError(it) }
                    .get()?.let { path to it }
            }
        }
        .awaitAll()
        .filterNotNull()
        .associate { (path, bytes) ->
            path.absolute() to createHash("sha1", bytes)
        }
        .plus(
            projectOverrides.associate { projectOverride ->
                projectOverride.fullOutputPath.absolute() to createHash("sha1", projectOverride.bytes)
            }
        )
    }

    val defaultIgnoredPaths = listOf("saves", "screenshots")

    val channel = produce { ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .mapNotNull { projectType ->
            val prjTypeDir = Path(workingPath, projectType.getPathString(configFile))
            if (prjTypeDir.notExists() ||
                defaultIgnoredPaths.any { it in prjTypeDir.pathString }) return@mapNotNull null

            prjTypeDir
        }
        .mapNotNull { dir ->
            dir.tryOrNull { path ->
                path.toFile().walkBottomUp().mapNotNull { file: File ->
                    file.toPath().takeIf { it != dir }
                }
            }
        }
        .forEach { pathSequence ->
            pathSequence.toSet().forEach { path ->
                launch {
                    val hash = path.readAndCreateSha1FromBytes()

                    if (!path.isDirectory() && path.extension !in listOf("jar", "zip")) return@launch

                    if (path.absolute() !in fileHashes.await().keys || hash !in fileHashes.await().values)
                    {
                        send(path)
                    }
                }
            }
        }
    }

    channel.consumeEach { path ->
        launch(Dispatchers.IO) {
            path.tryToResult {
                if (defaultIgnoredPaths.none { ignored -> ignored in it.pathString })
                {
                    it.deleteIfExists()
                }
            }.onSuccess {
                onSuccess(path)
            }.onFailure {
                onError(it)
            }
        }
    }

    launch {
        if (channel.isEmpty)
        {
            this.cancel()
        }
    }
}