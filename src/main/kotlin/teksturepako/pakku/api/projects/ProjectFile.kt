package teksturepako.pakku.api.projects

import kotlinx.serialization.Required
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
    open val requiredDependencies: MutableSet<String>? = null

    open val id: String = ""
    open val parentId: String = ""
}

@Serializable
sealed class ProjectFile(
    @Required val type: String
) : IProjectFile()

@Serializable
@SerialName("MrFile")
data class MrFile(
    @SerialName("file_name") override val fileName: String,
    @SerialName("mc_versions") override val mcVersions: MutableList<String>,
    override val loaders: MutableList<String>,
    @SerialName("release_type") override val releaseType: String,
    override var url: String?,

    override val id: String,
    @SerialName("parent_id") override val parentId: String,

    val hashes: MutableMap<String, String>? = null,
    @SerialName("required_dependencies") override val requiredDependencies: MutableSet<String>?,
) : ProjectFile(Modrinth.serialName)

@Serializable
@SerialName("CfFile")
data class CfFile(
    @SerialName("file_name") override val fileName: String,
    @SerialName("mc_versions") override val mcVersions: MutableList<String>,
    override val loaders: MutableList<String>,
    @SerialName("release_type") override val releaseType: String,
    override var url: String?,

    override val id: String,
    @SerialName("parent_id") override val parentId: String,

    @SerialName("required_dependencies") override val requiredDependencies: MutableSet<String>?,
) : ProjectFile(CurseForge.serialName)