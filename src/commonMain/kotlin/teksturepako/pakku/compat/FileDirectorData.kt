package teksturepako.pakku.compat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.compat.FileDirectorData.UrlEntry

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

fun Project.addToFileDirectorFrom(platform: Platform, fileDirector: FileDirectorData)
{
    if (!this.redistributable) return

    val url = this.getFilesForPlatform(platform).firstOrNull()?.url ?: return

    fileDirector.urlBundle.add(
        UrlEntry(
            url = url.replace(" ", "+"),
            folder = this.type.folderName
        )
    )
}