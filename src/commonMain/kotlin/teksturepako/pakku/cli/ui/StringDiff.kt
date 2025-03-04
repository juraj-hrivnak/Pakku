package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors

fun coloredStringDiff(oldVersion: String, newVersion: String): Pair<String, String>
{
    fun findCommonPrefix(s1: String, s2: String): Int =
        s1.zip(s2).takeWhile { (c1, c2) -> c1 == c2 }.count()

    fun findCommonSuffix(s1: String, s2: String): Int
    {
        val maxSuffixLength = minOf(s1.length, s2.length) - findCommonPrefix(s1, s2)
        return s1.reversed().zip(s2.reversed())
            .takeWhile { (c1, c2) -> c1 == c2 }
            .count()
            .coerceAtMost(maxSuffixLength)
    }

    val prefixLength = findCommonPrefix(oldVersion, newVersion)
    val suffixLength = findCommonSuffix(oldVersion, newVersion)

    val prefix = oldVersion.take(prefixLength)
    val oldDiff = oldVersion.substring(prefixLength, oldVersion.length - suffixLength)
    val newDiff = newVersion.substring(prefixLength, newVersion.length - suffixLength)
    val suffix = oldVersion.takeLast(suffixLength)

    val coloredOldVersion = prefix + TextColors.red(oldDiff) + suffix
    val coloredNewVersion = prefix + TextColors.green(newDiff) + suffix

    return coloredOldVersion to coloredNewVersion
}
