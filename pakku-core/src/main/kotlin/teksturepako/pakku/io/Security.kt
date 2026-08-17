package teksturepako.pakku.io

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
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

/** Validates relative entry paths (overrides, zip names, subpaths). Absolute roots are illegal. */
fun filterPath(path: String): Result<String, ActionError>
{
    return if (path.hasUnsafeRelativePathComponents()) Err(IllegalPath(path)) else Ok(path)
}

/**
 * True when [this] is a single path segment safe to use as a downloaded file name
 * (no separators, traversal, or reserved device names).
 */
fun String.isSafeFileName(): Boolean
{
    if (isBlank() || this == "." || this == "..") return false
    if ('/' in this || '\\' in this) return false
    return !hasUnsafeRelativePathComponents()
}

/**
 * Path safety for both absolute filesystem paths and relative entry names.
 *
 * - Relative: same rules as [filterPath] (no `/`, `\`, `..`, drive letters, device names).
 * - Absolute: roots/`C:` are allowed; still rejects `..` segments and Windows device names.
 */
fun Path.hasUnsafePathComponents(): Boolean =
    if (this.isAbsolute) this.hasUnsafeAbsolutePathComponents()
    else this.pathString.hasUnsafeRelativePathComponents()

private fun Path.hasUnsafeAbsolutePathComponents(): Boolean =
    this.any { component ->
        val name = component.pathString
        name == ".." || name.isWindowsDeviceName()
    }

private fun String.hasUnsafeRelativePathComponents(): Boolean
{
    return this.contains("..")
        || this.contains(Regex("[A-Z]:/"))
        || this.contains(Regex("[A-Z]:\\\\"))
        || this.startsWith("/")
        || this.startsWith("\\")
        || this.split(File.separator).any { it.isWindowsDeviceName() }
}

private fun String.isWindowsDeviceName(): Boolean =
    this.uppercase().matches(Regex("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\.|$)"))

fun Path.isWithinBounds(baseDir: Path): Boolean
{
    return try
    {
        val resolvedThis = this.resolveAgainstRealAncestor()
        val resolvedBase = baseDir.resolveAgainstRealAncestor()

        resolvedThis.startsWith(resolvedBase)
    }
    catch (_: Exception)
    {
        false
    }
}

/** Resolves symlinks in the existing part of a path while retaining any not-yet-created suffix. */
private fun Path.resolveAgainstRealAncestor(): Path
{
    var existingPath = normalize().toAbsolutePath()
    val missingComponents = mutableListOf<Path>()

    while (!Files.exists(existingPath, LinkOption.NOFOLLOW_LINKS))
    {
        missingComponents.add(existingPath.fileName ?: throw IllegalArgumentException("Path has no existing ancestor"))
        existingPath = existingPath.parent ?: throw IllegalArgumentException("Path has no existing ancestor")
    }

    var resolved = existingPath.toRealPath()
    for (component in missingComponents.asReversed())
    {
        resolved = resolved.resolve(component)
    }

    return resolved.normalize()
}
