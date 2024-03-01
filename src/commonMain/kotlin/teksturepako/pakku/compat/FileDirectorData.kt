package teksturepako.pakku.compat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileDirectorData(
    @SerialName("url") val urlBundle: MutableList<UrlEntry> = mutableListOf(),
    @SerialName("curse") val curseBundle: MutableList<CurseEntry> = mutableListOf()
)
{
    @Serializable
    data class UrlEntry(
        val url: String,
        val folder: String
    )

    @Serializable
    data class CurseEntry(
        val addonId: String,
        val fileId: String,
        val folder: String
    )
}
