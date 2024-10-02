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
import teksturepako.pakku.api.data.FetchHistoryFile
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
    val file = providers.firstNotNullOfOrNull { provider ->
        project.getFilesForProvider(provider).firstOrNull()
    }

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

    val fetchHistory = FetchHistoryFile.readOrNew()

    val fileHashes: Map<Path, String> = projectFiles
        .mapNotNull { projectFile ->
            val parentProject = projectFile.getParentProject(lockFile) ?: return@mapNotNull null

            val path = projectFile.getPath(parentProject, configFile)

            path.tryToResult { it.readBytes() }
                .onFailure { onError(it) }
                .get()?.let { path to it }
        }
        .associate { (path, bytes) ->
            path.absolute() to createHash("sha1", bytes)
        }
        .plus(
            projectOverrides.associate { projectOverride ->
                projectOverride.fullOutputPath.absolute() to createHash("sha1", projectOverride.bytes)
            }
        )

    @Suppress("ConvertCallChainIntoSequence")
    val channel = produce {
        ProjectType.entries
            .filterNot { it == ProjectType.WORLD }
            .mapNotNull { projectType ->
                val prjTypeDir = Path(workingPath, projectType.getPathString(configFile))
                if (prjTypeDir.notExists()) return@mapNotNull null

                prjTypeDir
            }
            .plus(
                fetchHistory.paths.mapNotNull { (_, path) ->
                    Path(workingPath, filterPath(path).get() ?: return@mapNotNull null)
                }
            )
            .mapNotNull { dir ->
                dir.tryOrNull {
                    it.toFile().walkBottomUp().map { file: File ->
                        file.toPath()
                    }
                }
            }
            .forEach { pathSequence ->
                pathSequence.toSet().mapNotNull x@ { path ->
                    val hash = path.readAndCreateSha1FromBytes()

                    if (path.extension !in listOf("jar", "zip")) return@x null

                    if (path.absolute() !in fileHashes.keys || hash !in fileHashes.values)
                    {
                        send(path)
                    }
                    else null
                }
            }
    }

    channel.consumeEach { path ->
        launch(Dispatchers.IO) {
            path.tryToResult { it.deleteIfExists() }.onSuccess {
                onSuccess(path)
            }
        }
    }

    launch {
        if (channel.isEmpty)
        {
            this.cancel()
        }

        ProjectType.entries.filterNot { it == ProjectType.WORLD }.map { projectType ->
            fetchHistory.paths[projectType.serialName] = projectType.getPathString(configFile)
        }

        fetchHistory.write()
    }
}