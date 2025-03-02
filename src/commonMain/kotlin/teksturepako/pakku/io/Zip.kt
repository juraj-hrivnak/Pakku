package teksturepako.pakku.io

import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.openZip
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolute
import kotlin.io.path.invariantSeparatorsPathString

fun readPathTextFromZip(zipPath: Path, filePath: Path): String? = runCatching {
    FileSystem.SYSTEM.openZip(zipPath.toOkioPath()).read(filePath.toOkioPath()) { readUtf8() }
}.get()

fun readPathTextFromZip(zipPath: Path, filePath: String): String? = runCatching {
    FileSystem.SYSTEM.openZip(zipPath.toOkioPath()).read(filePath.toPath()) { readUtf8() }
}.get()

suspend fun zip(inputDirectory: Path, outputZipFile: Path) = withContext(Dispatchers.IO) {
    val inputFile = inputDirectory.toFile()

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile.toFile()))).use { zos ->
        for (file in inputFile.walkTopDown())
        {
            val zipFileName = file.toPath().absolute().invariantSeparatorsPathString
                .removePrefix(file.toPath().absolute().invariantSeparatorsPathString)
                .removePrefix("/")

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