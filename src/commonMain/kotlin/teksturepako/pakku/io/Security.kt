package teksturepako.pakku.io

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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
    if (path.contains("..")
        || path.contains(Regex("[A-Z]:/"))
        || path.contains(Regex("[A-Z]:\\\\"))
        || path.startsWith("/")
        || path.startsWith("\\")
    ) return Err(IllegalPath(path))

    return Ok(path)
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

fun Path.hasUnsafePathComponents(): Boolean
{
    val pathString = this.toString()

    if (pathString.contains("..")) return true

    if (this.isAbsolute) return true

    return this.any { component ->
        val name = component.toString()
        // Reject components that are exactly ".." or "."
        name == ".." || name == "." ||
        // Reject null bytes (can cause issues in some systems)
        name.contains('\u0000') ||
        // Reject Windows device names (CON, PRN, AUX, NUL, COM1-9, LPT1-9)
        name.uppercase().matches(Regex("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.|$)"))
    }
}