package teksturepako.pakku.io

fun Long.toHumanReadableSize(): String
{
    val size = this.toDouble()
    return when {
        (size >= 1 shl 30) -> "%.1f GB".format(size / (1 shl 30))
        (size >= 1 shl 20) -> "%.1f MB".format(size / (1 shl 20))
        (size >= 1 shl 10) -> "%.0f kB".format(size / (1 shl 10))
        else               -> "$size bytes"
    }
}