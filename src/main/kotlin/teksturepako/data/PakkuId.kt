package teksturepako.data

import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

fun pakkuId(input: String): String
{
    return BigInteger(1, MessageDigest.getInstance("MD5")
        .digest(Random.nextBytes(input.toByteArray())))
        .toString(16).padStart(32, '0')
}