package teksturepako.projects

import kotlinx.serialization.Serializable

@Serializable
data class ProjectFile(
    val fileName: String,
    val mcVersion: String,
)
