@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package teksturepako.pakku.api.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.*
import teksturepako.pakku.api.platforms.Platform

/**
 * Represents a project. (E.g. a mod, resource pack, shader, etc.)
 *
 * @property pakkuId Pakku ID. Randomly generated and assigned when adding this project.
 * @property type The type of the project. (E.g. a mod, resource pack, shader, etc.)
 * @property side The side required by this project. Defaults to [ProjectSide.BOTH].
 * @property slug Mutable map of *platform name* to *project slug*, a short and lowercase version of the name.
 * @property name Mutable map of *platform name* to *project name*.
 * @property id Mutable map of *platform name* to *id*.
 * @property files Mutable set of associated files.
 */
@Serializable
data class Project(
    @SerialName("pakku_id") var pakkuId: String? = null,
    @SerialName("pakku_links") val pakkuLinks: MutableSet<String> = mutableSetOf(),
    val type: ProjectType,
    var side: ProjectSide? = null,

    val slug: MutableMap<String, String>,
    val name: MutableMap<String, String>,
    val id: MutableMap<String, String>,
    val files: MutableSet<ProjectFile>
)
{
    /**
     * Combines two projects of the same type into a new project.
     *
     * @param other The project to be combined with the current project.
     * @return A new [Project] object created by combining the data from the current and the provided project.
     * @throws PakkuException if projects have different types or pakku links.
     */
    operator fun plus(other: Project): Project
    {
        if (this.type != other.type) throw PakkuException("Can not combine two projects of different type!")
        if (other.pakkuLinks.isNotEmpty() && this.pakkuLinks != other.pakkuLinks)
            throw Exception("Can not combine two projects with different pakku links!")

        return Project(
            pakkuId = this.pakkuId,
            pakkuLinks = this.pakkuLinks,
            type = this.type,
            side = this.side,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),
            files = (this.files + other.files).toMutableSet(),
        )
    }

    /**
     * Combines two projects of the same type and returns a new project.
     *
     * @param other The project to combine with the current project.
     * @param replaceFiles If `true`, replaces files from the current project with files from the other project.
     * @return A new [Project] object created by combining the data from the current and the provided project.
     * @throws PakkuException if projects have different types or pakku links.
     */
    fun plus(other: Project, replaceFiles: Boolean): Project
    {
        if (this.type != other.type) throw Exception("Can not combine two projects of different type!")
        if (other.pakkuLinks.isNotEmpty() && this.pakkuLinks != other.pakkuLinks)
            throw Exception("Can not combine two projects with different pakku links!")

        return Project(
            pakkuId = this.pakkuId,
            pakkuLinks = this.pakkuLinks,
            type = this.type,
            side = this.side,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),
            files = if (replaceFiles) other.files else (this.files + other.files).toMutableSet(),
        )
    }

    /**
     * Checks if the current project contains at least one slug or ID from the specified project.
     *
     * @param other The project to check.
     * @return `true` if the current project contains at least one slug or ID from the specified project, otherwise `false`.
     */
    infix fun isAlmostTheSameAs(other: Project): Boolean
    {
        return this.slug.values.any { it in other.slug.values } || this.id.values.any { it in other.id.values }
    }

    /**
     * Checks if the current project contains the specified string in its slugs or IDs.
     *
     * @param input The string to check.
     * @return `true` if the current project contains the specified string, otherwise `false`.
     */
    operator fun contains(input: String): Boolean
    {
        return input in this.slug.values || input in this.id.values
    }

    /**
     * Checks if the project has any files.
     *
     * @return `true` if the project has files, and they are all not empty, otherwise `false`.
     */
    fun hasFiles(): Boolean = this.files.allNotEmpty()

    /**
     * Checks if the project has no files.
     *
     * @return `true` if the project has no files, or they are all empty, otherwise `false`.
     */
    fun hasNoFiles(): Boolean = this.files.allEmpty()

    /**
     * Checks if the project is associated with a specific platform.
     *
     * @param platform The platform to check.
     * @return `true` if the project is associated with the specified platform, otherwise `false`.
     */
    fun isOnPlatform(platform: Platform): Boolean
    {
        return platform.serialName in this.slug.keys
    }

    /**
     * Checks if the project is not associated with a specific platform.
     *
     * @param platform The platform to check.
     * @return `true` if the project is not associated with the specified platform, otherwise `false`.
     */
    fun isNotOnPlatform(platform: Platform): Boolean = !isOnPlatform(platform)

    /**
     * Checks if the project has files for a specific platform.
     *
     * @param platform The platform to check for files.
     * @return `true` if the project has files for the specified platform, otherwise `false`.
     */
    fun hasFilesForPlatform(platform: Platform): Boolean
    {
        return platform.serialName in this.files.map { it.type }
    }

    /**
     * Checks if the project has no files for a specific platform.
     *
     * @param platform The platform to check for files.
     * @return `true` if the project has no files for the specified platform, otherwise `false`.
     */
    fun hasNoFilesForPlatform(platform: Platform): Boolean = !hasFilesForPlatform(platform)

    /**
     * Checks if file names match across multiple platforms.
     *
     * @param platforms The list of platforms to compare.
     * @return `true` if the file names match across all specified platforms, otherwise `false`.
     */
    fun fileNamesMatchAcrossPlatforms(platforms: List<Platform>): Boolean
    {
        return this.files.asSequence()
            .map { it.fileName }
            .chunked(platforms.size)
            .all { it.allEqual() }
    }

    /**
     * Checks if file names do not match across multiple platforms.
     *
     * @param platforms The list of platforms to compare.
     * @return `true` if the file names do not match across all specified platforms, otherwise `false`.
     */
    fun fileNamesNotMatchAcrossPlatforms(platforms: List<Platform>): Boolean = !fileNamesMatchAcrossPlatforms(platforms)

    /**
     * Retrieves a list of project files associated with a specific platform.
     *
     * @param platform The platform for which to retrieve project files.
     * @return A list of [ProjectFile] instances associated with the specified platform.
     */
    fun getFilesForPlatform(platform: Platform): List<ProjectFile>
    {
        return this.files.filter { platform.serialName == it.type }
    }


    suspend fun requestDependencies(projectProvider: IProjectProvider): List<Project>
    {
        return this.files
            .flatMap { it.requiredDependencies ?: emptyList() }
            .mapNotNull {
                projectProvider.requestProjectWithFiles(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), it)
            }
    }


    init
    {
        // Init Pakku ID
        if (pakkuId == null) pakkuId = generatePakkuId()
        // Init ProjectSide
        if (side == null) side = ProjectSide.BOTH
    }
}