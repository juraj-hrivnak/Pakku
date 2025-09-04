package teksturepako.pakku.io

import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.io.FileAction.*
import java.nio.file.Path
import kotlin.io.path.*

/** Class to hold file information for hash comparison */
@JvmInline
private value class FileInfo private constructor(private val pathAndHash: Pair<Path, String?>)
{
    val relativePath: Path get() = pathAndHash.first
    val hash: String? get() = pathAndHash.second

    constructor(relativePath: Path, hash: String? = null) : this(relativePath to hash)
}

sealed class FileAction
{
    abstract val description: String

    data class FileCopied(
        val source: Path,
        val destination: Path,
        val hash: String?
    ) : FileAction()
    {
        override val description = "copied file from $source to $destination (hash: ${hash?.take(8)})"
    }

    data class FileDeleted(
        val path: Path,
        val hash: String?
    ) : FileAction()
    {
        override val description = "deleted file $path (hash mismatch: ${hash?.take(8)})"
    }

    data class DirectoryDeleted(
        val path: Path
    ) : FileAction()
    {
        override val description = "deleted empty directory $path"
    }
}

/**
 * Recursively copies a file or directory optimized by hash comparison.
 * Files are only copied when their hashes differ.
 */
suspend fun Path.copyRecursivelyTo(
    destination: Path,
    onAction: suspend (FileAction) -> Unit = { },
    cleanUp: Boolean = true,
): ActionError? = when
{
    this.isRegularFile() -> this.copyFileTo(destination, onAction)
    this.isDirectory()   -> this.copyDirectoryTo(destination, onAction, cleanUp)
    else                 -> InvalidPathError(this)
}

private suspend fun Path.copyFileTo(
    destination: Path,
    onAction: suspend (FileAction) -> Unit
): ActionError? = runCatching {
    val sourceHash = this.readAndCreateSha1FromBytes()
    val destHash = if (!destination.exists()) null else destination.readAndCreateSha1FromBytes()

    if (destHash == null || sourceHash != destHash)
    {
        destination.parent?.createDirectories()
        this.copyTo(destination, overwrite = true)
        onAction(FileCopied(source = this, destination = destination, hash = sourceHash))
    }

    null
}.getOrElse { CopyError(it.message ?: "Unknown error during file copy") }

private suspend fun Path.copyDirectoryTo(
    destination: Path,
    onAction: suspend (FileAction) -> Unit,
    cleanUp: Boolean,
): ActionError? = runCatching {

    val sourceFiles = this.collectFileInfo()
    val destinationFiles = destination.collectFileInfo()

    processFilesByHash(sourceFiles, destinationFiles, this, destination, onAction)
        ?.onError { return@runCatching it }

    if (cleanUp) cleanupByHash(destinationFiles, sourceFiles, destination, onAction)

    null
}.getOrElse { CopyError(it.message ?: "Unknown error during directory copy") }

@OptIn(ExperimentalPathApi::class)
private suspend fun Path.collectFileInfo(): List<FileInfo> = coroutineScope {
    walk()
        .filter { it.isRegularFile() }
        .toList()
        .mapAsync { path ->
            FileInfo(
                relativePath = this@collectFileInfo.relativize(path),
                hash = path.readAndCreateSha1FromBytes()
            )
        }
}

private suspend fun processFilesByHash(
    sourceFiles: List<FileInfo>,
    destinationFiles: List<FileInfo>,
    source: Path,
    destination: Path,
    onAction: suspend (FileAction) -> Unit,
): ActionError? = runCatching {
    val destFileMap = destinationFiles.associateBy { it.relativePath }

    sourceFiles.forEach { sourceFile ->
        val destFile = destFileMap[sourceFile.relativePath]

        if (destFile?.hash == null || destFile.hash != sourceFile.hash)
        {
            val destPath = destination.resolve(sourceFile.relativePath)
            destPath.parent?.createDirectories()
            source.resolve(sourceFile.relativePath).copyTo(destPath, overwrite = true)

            val action = FileCopied(source = sourceFile.relativePath, destination = destPath, hash = sourceFile.hash)

            onAction(action)
        }
    }

    null
}.getOrElse { CopyError(it.message ?: "Error processing files") }

@OptIn(ExperimentalPathApi::class)
private suspend fun cleanupByHash(
    destinationFiles: List<FileInfo>,
    sourceFiles: List<FileInfo>,
    destination: Path,
    onAction: suspend (FileAction) -> Unit,
)
{
    val sourceFileMap = sourceFiles.associateBy { it.relativePath }

    destinationFiles
        .filterNot { destFile ->
            sourceFileMap[destFile.relativePath]?.hash == destFile.hash
        }
        .forEach { fileInfo ->
            val fileToDelete = destination.resolve(fileInfo.relativePath)
            fileToDelete.deleteIfExists()

            val action = FileDeleted(path = fileInfo.relativePath, hash = fileInfo.hash)

            onAction(action)
        }

    // Clean up empty directories bottom-up
    destination.walk(PathWalkOption.BREADTH_FIRST, PathWalkOption.INCLUDE_DIRECTORIES)
        .filter { it.isDirectory() && it != destination }
        .forEach { dir ->
            if (dir.listDirectoryEntries().isEmpty()) {
                dir.deleteIfExists()
                onAction(DirectoryDeleted(dir))
            }
        }
}

data class InvalidPathError(val path: Path) : ActionError()
{
    override val rawMessage = "Path $path is neither a file nor a directory"
}

data class CopyError(override val rawMessage: String) : ActionError()
