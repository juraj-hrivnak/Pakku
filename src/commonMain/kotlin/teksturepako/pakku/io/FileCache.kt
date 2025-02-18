package teksturepako.pakku.io

import com.github.michaelbull.result.*
import teksturepako.pakku.api.actions.errors.ActionError
import java.nio.file.Path
import kotlin.io.path.*

suspend fun cleanUpDirectory(
    inputDirectory: Path,
    cachedPaths: List<Path>,
    ignoredPaths: List<Path> = listOf(),
    onError: suspend (ActionError) -> Unit,
    onAction: suspend (String) -> Unit,
)
{
    // Map every path not in a directory to absolute path and its hash
    val fileHashes: Map<Path, String> = cachedPaths.filterNot { it.isDirectory() }
        .mapNotNull { file ->
            file.tryToResult { it.readBytes() }
                .onFailure { error -> onError(error) }
                .get()
                ?.let { file to it }
        }
        .associate { it.first.absolute() to createHash("sha1", it.second) }

    // Map every path in a directory to absolute path and its hash
    val dirContentHashes: Map<Path, String> = cachedPaths.filter { it.isDirectory() }
        .mapNotNull { directory ->
            directory.tryToResult { it.toFile().walkTopDown() }
                .onFailure { error -> onError(error) }.get()
        }
        .flatMap {
            it.mapNotNull { file -> file.toPath() }
        }
        .mapNotNull { path ->
            path.readAndCreateSha1FromBytes()?.let {
                path.absolute() to it
            }
        }
        .toMap()

    val fileTreeWalk = inputDirectory.tryToResult { it.toFile().walkBottomUp() }
        .onFailure { error -> onError(error) }.get() ?: return

    for (file in fileTreeWalk)
    {
        val path = file.toPath()
        if (path == inputDirectory) continue
        if (path.absolute() in ignoredPaths.map { it.absolute() }) continue

        if (path.isDirectory())
        {
            val currentDirFiles = path.tryToResult { it.toFile().listFiles() }
                .onFailure { error -> onError(error) }
                .get()
                ?.mapNotNull { it.toPath() }
                ?: continue

            if (currentDirFiles.isNotEmpty()) continue

            val deleted = path.tryOrNull { deleteIfExists() }
            if (deleted == true) onAction("delete empty directory $path")
        }
        else
        {
            val hash = path.readAndCreateSha1FromBytes() ?: continue

            if ((path.absolute() in fileHashes.keys && hash in fileHashes.values) ||
                (path.absolute() in dirContentHashes.keys && hash in dirContentHashes.values)) continue

            val deleted = path.tryOrNull { deleteIfExists() }
            if (deleted == true) onAction("delete file $path")
        }
    }
}

suspend fun cleanUpDirectoryRelative(
    inputDirectory: Path,
    cachedPaths: List<Path>,
    ignoredPaths: List<Path> = listOf(),
    onError: suspend (ActionError) -> Unit,
    onAction: suspend (String) -> Unit,
)
{
    val basePath = cachedPaths
        .firstOrNull { cachedPath ->
            cachedPath.pathString.contains(inputDirectory.fileName.pathString)
        }
        ?.parent
        ?: return

    // Map every path not in a directory to its path relative to base and its hash
    val fileHashes: Map<Path, String> = cachedPaths.filterNot { it.isDirectory() }
        .mapNotNull { path ->
            path.tryToResult { it.readBytes() }
                .onFailure { error -> onError(error) }
                .get()
                ?.let { path to it }
        }
        .associate { (path, bytes) ->
            basePath.relativize(path) to createHash("sha1", bytes)
        }

    // Map every path in a directory to its path relative to base and its hash
    val dirContentHashes: Map<Path, String> = cachedPaths.filter { it.isDirectory() }
        .mapNotNull { directory ->
            directory.tryToResult { it.toFile().walkTopDown() }
                .onFailure { error -> onError(error) }.get()
        }
        .flatMap {
            it.mapNotNull { file -> file.toPath() }
        }
        .mapNotNull { path ->
            path.readAndCreateSha1FromBytes()?.let {
                basePath.relativize(path) to it
            }
        }
        .toMap()

    val fileTreeWalk = inputDirectory.tryToResult { it.toFile().walkBottomUp() }
        .onFailure { error -> onError(error) }.get() ?: return

    for (file in fileTreeWalk)
    {
        val path = file.toPath()
        val relativePath = inputDirectory.parent?.relativize(path) ?: continue

        if (path == inputDirectory) continue
        if (relativePath in ignoredPaths.map { inputDirectory.parent?.relativize(it) }) continue

        if (path.isDirectory())
        {
            val currentDirFiles = path.tryToResult { it.toFile().listFiles() }
                .onFailure { error -> onError(error) }
                .get()
                ?.mapNotNull { it.toPath() }
                ?: continue

            if (currentDirFiles.isNotEmpty()) continue

            val deleted = path.tryOrNull { deleteIfExists() }
            if (deleted == true) onAction("delete empty directory $path")
        }
        else
        {
            val hash = path.readAndCreateSha1FromBytes() ?: continue

            if ((relativePath in fileHashes.keys && hash in fileHashes.values) ||
                (relativePath in dirContentHashes.keys && hash in dirContentHashes.values)) continue

            val deleted = path.tryOrNull { deleteIfExists() }
            if (deleted == true) onAction("delete file $path")
        }
    }
}
