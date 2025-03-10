package teksturepako.pakku.api.actions.fetch

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.DirectoryNotEmpty
import teksturepako.pakku.api.actions.sync.detectProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

enum class DeletionActionType(val result: String)
{
    DELETE("deleted"), SHELF("shelved")
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun deleteOldFiles(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (file: Path, actionType: DeletionActionType) -> Unit,
    projectFiles: List<ProjectFile>,
    projectOverrides: Set<ProjectOverride>,
    lockFile: LockFile,
    configFile: ConfigFile?,
    platforms: List<Platform>,
    shelve: Boolean = false,
) = coroutineScope {

    val defaultIgnoredPaths = listOf("saves", "screenshots")
    val allowedExtensions = listOf(".jar", ".zip", ".jar.meta")

    val detectedProjects = async {
        detectProjects(lockFile, configFile, platforms)
            .flatMap { project -> project.files }
            .map { projectFile ->
                async x@ {
                    val parentProject = projectFile.getParentProject(lockFile) ?: return@x null

                    val path = projectFile.getPath(parentProject, configFile)

                    readPathBytesToResult(path).get()?.let { path to it }
                }
            }
            .awaitAll()
            .filterNotNull()
            .associate { (path, bytes) ->
                path.absolute() to createHash("sha1", bytes)
            }
    }

    val fileHashes = async { projectFiles
        .map { projectFile ->
            async x@ {
                val parentProject = projectFile.getParentProject(lockFile) ?: return@x null

                val path = projectFile.getPath(parentProject, configFile)

                readPathBytesToResult(path)
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

    val channel = produce { ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .mapNotNull { projectType ->
            val prjTypeDir = Path(workingPath, projectType.getPathString(configFile))
            if (prjTypeDir.notExists() || defaultIgnoredPaths.any { it in prjTypeDir.pathString })
                return@mapNotNull null

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

                    if (!path.isDirectory() && allowedExtensions.none { path.pathString.endsWith(it) }) return@launch

                    if (path.absolute() !in fileHashes.await().keys || hash !in fileHashes.await().values)
                    {
                        send(path to hash)
                    }
                }
            }
        }
    }

    channel.consumeEach { (path, hash) ->
        launch(Dispatchers.IO) {
            if (defaultIgnoredPaths.any { ignored -> ignored in path.pathString }) return@launch

            if (shelve)
            {
                if (path.absolute() in detectedProjects.await().keys
                    || hash in detectedProjects.await().values
                    || path.isDirectory()) return@launch

                path.tryToResult {
                    Dirs.shelfDir.createDirectories()
                    val newFile = Path(Dirs.shelfDir.pathString, it.fileName.pathString)
                    moveTo(newFile)
                }.onSuccess {
                    onSuccess(path, DeletionActionType.SHELF)
                }.onFailure { error ->
                    if (error !is DirectoryNotEmpty) onError(error)
                }
            }
            else
            {
                path.tryToResult {
                    deleteIfExists()
                }.onSuccess {
                    onSuccess(path, DeletionActionType.DELETE)
                }.onFailure { error ->
                    if (error !is DirectoryNotEmpty) onError(error)
                }
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
