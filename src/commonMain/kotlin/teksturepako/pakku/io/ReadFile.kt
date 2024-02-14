package teksturepako.pakku.io

import korlibs.io.file.std.localCurrentDirVfs

suspend fun readFileOrNull(path: String): String?
{
    val file = localCurrentDirVfs[path]

    return if (file.exists()) {
        runCatching { file.readString() }.getOrNull()
    } else null
}