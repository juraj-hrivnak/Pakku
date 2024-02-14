package teksturepako.pakku.api.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @property name The name of this version
 * @property versionNumber The version number, ideally following semantic versioning
 * @property changelog The changelog for this version (nullable)
 * @property dependencies List of specific versions of projects that this version depends on (Array of VersionDependency objects)
 * @property gameVersions List of versions of Minecraft that this version supports
 * @property versionType Enum representing the release channel for this version: "release", "beta", "alpha"
 * @property loaders List of mod loaders that this version supports
 * @property featured Whether the version is featured or not
 * @property status Enum representing the status of this version: "listed", "archived", "draft", "unlisted", "scheduled", "unknown"
 * @property requestedStatus Enum representing the requested status for this version: "listed", "archived", "draft", "unlisted" (nullable)
 * @property id The ID of the version, encoded as a base62 string
 * @property projectId The ID of the project this version is for
 * @property authorId The ID of the author who published this version
 * @property datePublished The date and time this version was published (string <ISO-8601>)
 * @property downloads The number of times this version has been downloaded
 * @property files List of files available for download for this version (Array of VersionFile objects)
 */
@Serializable
data class MrVersionModel(
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    val changelog: String? = null,
    val dependencies: List<VersionDependency> = listOf(),
    @SerialName("game_versions") val gameVersions: List<String> = listOf(),
    @SerialName("version_type") val versionType: String,
    val loaders: List<String> = listOf(),
    val featured: Boolean,
    val status: String,
    @SerialName("requested_status") val requestedStatus: String? = null,
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("date_published") val datePublished: String,
    val downloads: Int,
    val files: List<VersionFile> = listOf()
) {
    /**
     * @property versionId The ID of the version that this version depends on (nullable)
     * @property projectId The ID of the project that this version depends on (nullable)
     * @property fileName The file name of the dependency, mostly used for showing external dependencies on modpacks (nullable)
     * @property dependencyType Enum representing the type of dependency that this version has: "required", "optional", "incompatible", "embedded"
     */
    @Serializable
    data class VersionDependency(
        @SerialName("version_id") val versionId: String? = null,
        @SerialName("project_id") val projectId: String? = null,
        @SerialName("file_name") val fileName: String? = null,
        @SerialName("dependency_type") val dependencyType: String
    )

    /**
     * @property hashes Map of hashes of the file. The key is the hashing algorithm and the value is the string version of the hash (object VersionFileHashes)
     * @property url Direct link to the file
     * @property filename Name of the file
     * @property primary Whether this file is the primary one for its version. Only a maximum of one file per version will have this set to true. If there are not any primary files, it can be inferred that the first file is the primary one.
     * @property size Size of the file in bytes
     * @property fileType Enum representing the type of the additional file, used mainly for adding resource packs to datapacks: "required-resource-pack", "optional-resource-pack" (nullable)
     */
    @Serializable
    data class VersionFile(
        val hashes: VersionFileHashes,
        val url: String,
        val filename: String,
        val primary: Boolean,
        val size: Int,
        @SerialName("file_type") val fileType: String? = null
    ) {
        @Serializable
        data class VersionFileHashes(
            val sha512: String,
            val sha1: String
        )
    }
}