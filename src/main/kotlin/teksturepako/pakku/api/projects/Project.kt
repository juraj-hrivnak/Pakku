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

    @SerialName("update_strategy") var updateStrategy: UpdateStrategy = UpdateStrategy.LATEST,
    @SerialName("can_redistribute") var canRedistribute: Boolean = true,

    var files: MutableSet<ProjectFile>
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
        if (this.type != other.type)
            throw PakkuException("Can not combine two projects of different type! $this ${this.type} + $other ${other.type}")
        if (other.pakkuLinks.isNotEmpty() && this.pakkuLinks != other.pakkuLinks)
            throw Exception("Can not combine two projects with different pakku links! $this ${this.type} + $other ${other.type}")

        return Project(
            pakkuId = this.pakkuId,
            pakkuLinks = this.pakkuLinks,
            type = this.type,
            side = this.side,

            name = (this.name + other.name).toMutableMap(),
            slug = (this.slug + other.slug).toMutableMap(),
            id = (this.id + other.id).toMutableMap(),

            updateStrategy = this.updateStrategy,
            canRedistribute = this.canRedistribute,

            files = (this.files + other.files).toMutableSet(),
        )
    }

    /** Checks if the current project contains at least one slug, ID or name from the other project. */
    infix fun isAlmostTheSameAs(other: Project): Boolean
    {
        return this.id.values.any { it in other.id.values }
                || this.name.values.any { it in other.name.values }
                || this.slug.values.any { it in other.slug.values }
    }

    /** Checks if the current project contains the specified string in its slugs or IDs. */
    operator fun contains(input: String): Boolean
    {
        return input in this.slug.values || input in this.id.values
    }

    /** Checks if the project has any files. */
    fun hasFiles(): Boolean = this.files.allNotEmpty()

    /** Checks if the project has no files. */
    fun hasNoFiles(): Boolean = this.files.allEmpty()

    /** Checks if the project is associated with the specified [platform][Platform]. */
    fun isOnPlatform(platform: Platform): Boolean = platform.serialName in this.slug.keys

    /** Checks if the project is not associated with the specified [platform][Platform]. */
    fun isNotOnPlatform(platform: Platform): Boolean = !isOnPlatform(platform)

    /** Checks if the project has files on the specified [platform][Platform]. */
    fun hasFilesOnPlatform(platform: Platform): Boolean
    {
        return platform.serialName in this.files.map { it.type }
    }

    /** Checks if the project has no files on the specified [platform][Platform]. */
    fun hasNoFilesOnPlatform(platform: Platform): Boolean = !hasFilesOnPlatform(platform)

    /** Checks if file names match across specified [platforms][platforms]. */
    fun fileNamesMatchAcrossPlatforms(platforms: List<Platform>): Boolean
    {
        return this.files.asSequence()
            .map { it.fileName }
            .chunked(platforms.size)
            .all { it.allEqual() }
    }

    /** Checks if file names do not match across specified [platforms][platforms]. */
    fun fileNamesDoNotMatchAcrossPlatforms(platforms: List<Platform>): Boolean =
        !fileNamesMatchAcrossPlatforms(platforms)

    /** Retrieves a list of [project files][ProjectFile] associated with the specified [platform][Platform]. */
    fun getFilesForPlatform(platform: Platform): List<ProjectFile>
    {
        return this.files.filter { platform.serialName == it.type }
    }

    /**
     * Requests [projects with files][IProjectProvider.requestProjectWithFiles] for all dependencies of this project.
     * @return List of [dependencies][Project].
     */
    suspend fun requestDependencies(projectProvider: IProjectProvider, pakkuLock: PakkuLock): List<Project>
    {
        return this.files
            .flatMap { it.requiredDependencies ?: emptyList() }
            .mapNotNull {
                projectProvider.requestProjectWithFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), it)
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