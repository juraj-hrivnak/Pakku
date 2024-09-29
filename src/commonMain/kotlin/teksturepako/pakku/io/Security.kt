package teksturepako.pakku.io

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
fun createHash(type: String, input: ByteArray): String =
    MessageDigest
        .getInstance(type.uppercase())
        .digest(input)
        .toHexString()

class IllegalPath(path: String) : ActionError("Illegal path: '$path'.")

fun filterPath(path: String): Result<String, ActionError>
{
    if (path.contains("..")
        || path.contains("[A-Z]:/".toRegex())
        || path.contains("[A-Z]:\\\\".toRegex())
        || path.startsWith("/")
        || path.startsWith("\\\\")
    ) return Err(IllegalPath(path))

    return Ok(path)
}