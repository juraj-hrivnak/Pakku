package teksturepako.pakku.api.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.FileNotFound
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

/** Returns the projects visible to a fork without writing parent projects into its local lock file. */
suspend fun LockFile.withForkParent(): Result<LockFile, ActionError>
{
    val config = if (ConfigFile.exists()) ConfigFile.readToResult() else Ok(ConfigFile())
    return config.flatMap { configFile ->
        if (configFile.parent == null) return@flatMap Ok(this)

        val path = parentLockFilePath()
            ?: return@flatMap Err(FileNotFound(Dirs.parentDir.resolve(LockFile.FILE_NAME).toString()))
        LockFile.readToResultFrom(path, inheritConfig = false)
            .map { it.mergedWithLocal(this, configFile) }
    }
}
