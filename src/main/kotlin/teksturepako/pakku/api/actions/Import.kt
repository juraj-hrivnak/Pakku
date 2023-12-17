package teksturepako.pakku.api.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.modpacks.CfManifest
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files

suspend fun importCfManifest(path: String): CfManifest?
{
    val manifest = File(path)

    return if (manifest.exists()) try
    {
        json.decodeFromString<CfManifest>(readFile(manifest, Charsets.UTF_8))
    } catch (e: Exception)
    {
        null
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