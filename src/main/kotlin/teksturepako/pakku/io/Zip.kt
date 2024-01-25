package teksturepako.pakku.io

import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.openAsZip

suspend fun unzip(path: String)
{
    val file = localCurrentDirVfs[path]

    file.openAsZip().listNames().forEach(::println)
    println()
}