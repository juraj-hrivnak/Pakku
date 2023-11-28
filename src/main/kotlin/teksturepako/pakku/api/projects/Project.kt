@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package teksturepako.pakku.api.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.allEmpty
import teksturepako.pakku.api.data.allEqual
import teksturepako.pakku.api.data.allNotEmpty
import teksturepako.pakku.api.data.generatePakkuId
import teksturepako.pakku.api.platforms.Platform

/**
 * Project.
 */
@Serializable
data class Project(
    /**
     * Pakku ID. Randomly generated. Assigned when adding this project.
     */
    @SerialName("pakku_id") var pakkuId: String? = null,

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
    val files: MutableSet<ProjectFile>
)
{
    /**
     * Combines two projects of the same type into a new project.
     *
     * @param other The project to be combined with the current project.
     * @return A new [Project] object created by combining the data from the current and the provided project.
     * @throws Exception if the projects have different types.
     */
    operator fun plus(other: Project): Project
    {
        if (this.type != other.type) throw Exception("Can not combine two projects of different type!")

        return Project(
            pakkuId = this.pakkuId,
            type = this.type,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),
            files = (this.files + other.files).toMutableSet(),
        )
    }


    fun plus(other: Project, replaceFiles: Boolean): Project
    {
        if (this.type != other.type) throw Exception("Can not combine two projects of different type!")

        return Project(
            pakkuId = this.pakkuId,
            type = this.type,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),
            files = if (replaceFiles) other.files else (this.files + other.files).toMutableSet(),
        )
    }

    operator fun contains(other: Project): Boolean
    {
        return this.slug.values.any { it in other.slug.values } || this.id.values.any { it in other.id.values }
    }

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
    fun isOnPlatform(platform: Platform): Boolean {
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
    fun hasFilesForPlatform(platform: Platform): Boolean {
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
    fun fileNamesMatchAcrossPlatforms(platforms: List<Platform>): Boolean {
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


    fun getFilesForPlatform(platform: Platform): List<ProjectFile>
    {
        return this.files.filter { platform.serialName == it.type }
    }


    init
    {
        // Init Pakku ID
        if (pakkuId == null) pakkuId = generatePakkuId()
    }
}