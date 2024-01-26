package teksturepako.pakku.api.projects

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("project_file")
data class ProjectFile(
    @Required val type: String,
    @SerialName("file_name") val fileName: String = "",
    @SerialName("mc_versions") val mcVersions: MutableList<String> = mutableListOf(),
    val loaders: MutableList<String> = mutableListOf(),
    @SerialName("release_type") val releaseType: String = "",
    var url: String? = null,
    val id: String = "",
    @SerialName("parent_id") val parentId: String = "",
    val hashes: MutableMap<String, String>? = null,
    @SerialName("required_dependencies") val requiredDependencies: MutableSet<String>? = null,
    val size: Int = 0,
)
