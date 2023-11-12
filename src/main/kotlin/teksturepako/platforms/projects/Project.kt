package teksturepako.platforms.projects

/**
 * Project (can be Mod, ResourcePack, etc.)
 */
data class Project(
    /**
     * Project name.
     */
    val name: String,

    /**
     * Project slug (short and lowercase version of the name).
     */
    val slug: String,

    /**
     * The type of the project. (Mod, ResourcePack, etc.)
     */
    val type: ProjectType,
)
