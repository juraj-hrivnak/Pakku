package teksturepako.pakku.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.debug
import java.io.File

actual suspend fun zipModpack(
    path: String?,
    outputFileName: String,
    extension: String,
    overrides: List<Pair<String?, List<String>>>,
    vararg create: Pair<String, Any>
): Result<String> = withContext(Dispatchers.IO) {

    val pakkuTemp = "./.pakku/.tmp"
    val output = "$outputFileName.$extension"

    if (path != null && !File(path).isDirectory)
        return@withContext Result.failure(PakkuException("Zip.jvm.kt#zipFile: $path is not a valid path"))

    File(pakkuTemp).deleteRecursively()

    val outputFile = if (path != null) File(path, output) else File(output)
    if (outputFile.exists()) outputFile.delete()

    for (pair in create)
    {
        val file = File("$pakkuTemp/${pair.first}")
        file.parentFile.mkdirs()
        file.createNewFile()

        if (pair.second is ByteArray && file.isFile)
        {
            debug { println("Zip.jvm.kt#zipFile: Creating temporary (ByteArray) file: ${file.path}") }
            file.writeBytes(pair.second as ByteArray)
        }
        else if (pair.second is String && file.isFile)
        {
            debug { println("Zip.jvm.kt#zipFile: Creating temporary (String) file: ${file.path}") }
            file.writeText(pair.second as String)
        }
    }

    val zip = ZipFile(outputFile)

    for (file in File(pakkuTemp).listFiles())
    {
        if (file.isFile) zip.addFile(file)
        else if (file.isDirectory) zip.addFolder(file)
    }

    // -- OVERRIDES --

    for ((ovFolderName, ovNames) in overrides)
    {
        for (ovName in ovNames)
        {
            if (File(ovName).exists())
            {
                zip.addFolder(File(ovName))

                if (ovFolderName != null)
                {
                    zip.renameFile("$ovName/", "$ovFolderName/$ovName/")
                }
            }
            else
            {
                return@withContext Result.failure(PakkuException("($ovFolderName) Override '$ovName' could not be found"))
            }
        }
    }

    File(pakkuTemp).deleteRecursively()

    return@withContext Result.success(outputFile.path)
}
