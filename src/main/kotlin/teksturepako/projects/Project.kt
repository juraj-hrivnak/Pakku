package teksturepako.projects

import kotlinx.serialization.Serializable

/**
 * Project (can be Mod, ResourcePack, etc.)
 */
@Serializable
data class Project(
    /**
     * Project slug. (Short and lowercase version of the name.)
     */
    val slug: String,

    /**
     * Map of *platform name* to *project name*.
     */
    val name: MutableMap<String, String>,

    /**
     * The type of the project. (Mod, ResourcePack, etc.)
     */
    val type: ProjectType,

    /**
     * Map of *platform name* to *id*.
     */
    val id: MutableMap<String, String>,

    /**
     * Map of *platform name* to associated files.
     */
    val files: MutableMap<String, List<ProjectFile>>
)