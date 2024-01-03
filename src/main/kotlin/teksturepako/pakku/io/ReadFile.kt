package teksturepako.pakku.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files

suspend fun readFileOrNull(file: File, encoding: Charset = Charsets.UTF_8): String?
{
    return if (file.exists()) {
        runCatching { readFile(file, encoding) }.getOrNull()
    } else null
}

@Throws(IOException::class)
private suspend fun readFile(file: File, encoding: Charset): String
{
    val encoded = withContext(Dispatchers.IO) {
        Files.readAllBytes(file.toPath())
    }
    return String(encoded, encoding)
}