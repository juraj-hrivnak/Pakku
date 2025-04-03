package teksturepako.pakku.io

import java.nio.file.Path

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