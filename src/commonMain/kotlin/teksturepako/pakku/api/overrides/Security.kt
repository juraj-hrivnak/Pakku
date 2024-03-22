package teksturepako.pakku.api.overrides

import java.security.MessageDigest

@OptIn(ExperimentalStdlibApi::class)
fun createHash(type: String, input: ByteArray): String =
    MessageDigest
        .getInstance(type.uppercase())
        .digest(input)
        .toHexString()

fun filterPath(path: String): String?
{
    if (path.contains("..")
        || path.contains("[A-Z]:/".toRegex())
        || path.contains("[A-Z]:\\\\".toRegex())
        || path.startsWith("/")
        || path.startsWith("\\\\")
    ) return null

    return path
}