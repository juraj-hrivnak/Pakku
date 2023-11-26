package teksturepako.pakku.api.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth

abstract class IProjectFile
{
    open val fileName: String = ""
    open val mcVersions: MutableList<String> = mutableListOf()
    open val loaders: MutableList<String> = mutableListOf()
    open val releaseType: String = ""
    open var url: String? = null
}

@Serializable
sealed class ProjectFile(
    val type: String
) : IProjectFile()

@Serializable
@SerialName("MrFile")
data class MrFile(
    @SerialName("file_name") override val fileName: String,
    @SerialName("mc_versions") override val mcVersions: MutableList<String>,
    override val loaders: MutableList<String>,
    @SerialName("release_type") override val releaseType: String,
    override var url: String?,

    val hashes: MutableMap<String, String>? = null
) : ProjectFile(Modrinth.serialName)

@Serializable
@SerialName("CfFile")
data class CfFile(
    @SerialName("file_name") override val fileName: String,
    @SerialName("mc_versions") override val mcVersions: MutableList<String>,
    override val loaders: MutableList<String>,
    @SerialName("release_type") override val releaseType: String,
    override var url: String?,

    val id: Int
) : ProjectFile(CurseForge.serialName)