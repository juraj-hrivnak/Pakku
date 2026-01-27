package teksturepako.pakku.api.data

import kotlin.random.Random

fun generatePakkuId(slug: MutableMap<String, String>): String
{
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    val slugToString = slug.toSortedMap().entries.joinToString("") { "${it.key}=${it.value}" }
    val seed = Random(slugToString.hashCode())

    return (1..16).map { allowedChars.random(seed) }.joinToString("")
}
