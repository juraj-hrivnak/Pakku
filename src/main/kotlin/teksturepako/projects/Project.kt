package teksturepako.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Project.
 */
@Serializable
data class Project(
    /**
     * Pakku ID. Randomly generated. Assigned when adding this project.
     */
    @SerialName("pakku_id")
    var pakkuId: String? = null,

    /**
     * The type of the project. Can be a mod, resource rack, etc.
     */
    val type: ProjectType,

    /**
     * Project slug. A short and lowercase version of the name.
     */
    val slug: MutableMap<String, String>,

    /**
     * Map of *platform name* to *project name*.
     */
    val name: MutableMap<String, String>,

    /**
     * Map of *platform name* to *id*.
     */
    val id: MutableMap<String, String>,

    /**
     * Map of *platform name* to associated files.
     */
    val files: MutableList<ProjectFile>
) {
    operator fun plus(other: Project): Project
    {
        return Project(
            pakkuId = pakkuId,
            type = this.type,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),
            files = (this.files + other.files).toMutableList(),
        )
    }
}