package teksturepako.pakku.io

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.io.path.pathString

@OptIn(ExperimentalStdlibApi::class)
fun createHash(type: String, input: ByteArray): String
{
    val hashType = when (type.uppercase())
    {
        "MD_2", "MD-2", "MD2"          -> "MD2"
        "MD_5", "MD-5", "MD5"          -> "MD5"
        "SHA_1", "SHA-1", "SHA1"       -> "SHA-1"
        "SHA_256", "SHA-256", "SHA256" -> "SHA-256"
        "SHA_384", "SHA-384", "SHA384" -> "SHA-384"
        "SHA_512", "SHA-512", "SHA512" -> "SHA-512"
        else                           -> throw NoSuchAlgorithmException(type)
    }

    return MessageDigest
        .getInstance(hashType)
        .digest(input)
        .toHexString()
}

class IllegalPath(path: String) : ActionError()
{
    override val rawMessage = "Illegal path: '$path'."
}

fun filterPath(path: String): Result<String, ActionError>
{
    return if (path.hasUnsafePathComponents()) Err(IllegalPath(path)) else Ok(path)
}

fun Path.hasUnsafePathComponents(): Boolean =
    this.pathString.hasUnsafePathComponents()

private fun String.hasUnsafePathComponents(): Boolean
{
    return this.contains("..")
        || this.contains(Regex("[A-Z]:/"))
        || this.contains(Regex("[A-Z]:\\\\"))
        || this.startsWith("/")
        || this.startsWith("\\")
        || this.split(File.separator).any { // windows devices
            it.uppercase().matches(Regex("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.|$)"))
        }
}

fun Path.isWithinBounds(baseDir: Path): Boolean
{
    return try
    {
        val normalizedThis = this.normalize().toAbsolutePath()
        val normalizedBase = baseDir.normalize().toAbsolutePath()

        normalizedThis.startsWith(normalizedBase)
    }
    catch (_: Exception)
    {
        false
    }
}
