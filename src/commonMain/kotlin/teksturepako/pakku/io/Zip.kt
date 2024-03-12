package teksturepako.pakku.io

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.openAsZip

suspend fun unzip(path: String): VfsFile
{
    val file = localCurrentDirVfs[path]
    return file.openAsZip()
}

expect suspend fun zipFile(
    path: String?,
    outputFileName: String,
    extension: String,
    overrides: List<String>,
    vararg create: Pair<String, Any>
): Result<String>