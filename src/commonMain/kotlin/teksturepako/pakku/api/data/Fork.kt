package teksturepako.pakku.api.data

import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.inputStream

fun sha256(path: Path): String
{
    val digest = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true)
        {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

fun parentLockFilePath(parentDir: Path = Dirs.parentDir): Path? =
    parentDir.resolve(LockFile.FILE_NAME).takeIf { it.exists() }

fun parentConfigFilePath(parentDir: Path = Dirs.parentDir): Path? =
    parentDir.resolve(ConfigFile.FILE_NAME).takeIf { it.exists() }
