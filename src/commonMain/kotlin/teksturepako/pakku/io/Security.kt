package teksturepako.pakku.io

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
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
        else -> throw NoSuchAlgorithmException(type)
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
        || path.contains("[A-Z]:/".toRegex())
        || path.contains("[A-Z]:\\\\".toRegex())
        || path.startsWith("/")
        || path.startsWith("\\")
    ) return Err(IllegalPath(path))

    return Ok(path)
}