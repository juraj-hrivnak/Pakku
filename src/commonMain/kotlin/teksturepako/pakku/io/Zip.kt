package teksturepako.pakku.io

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.openAsZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

suspend fun unzip(path: String): VfsFile
{
    val file = localCurrentDirVfs[path]
    return file.openAsZip()
}

suspend fun zip(inputDirectory: Path, outputZipFile: Path) = withContext(Dispatchers.IO) {
    val inputFile = inputDirectory.toFile()

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile.toFile()))).use { zos ->
        for (file in inputFile.walkTopDown())
        {
            val zipFileName = file.absolutePath.removePrefix(inputFile.absolutePath).removePrefix(File.separator)

            if (zipFileName.isBlank()) continue

            val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")

            zos.putNextEntry(entry)

            if (file.isFile)
            {
                file.inputStream().use { fis -> fis.copyTo(zos) }
            }
        }
    }
}