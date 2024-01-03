package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable
import teksturepako.pakku.api.models.CfMod.ModAsset
import teksturepako.pakku.api.models.CfMod.ModLinks

/**
 * @property id [integer(int32)] The mod id
 * @property gameId [integer(int32)] The game id this mod is for
 * @property name [String] The name of the mod
 * @property slug [String] The mod slug that would appear in the URL
 * @property links [ModLinks] Relevant links for the mod such as Issue tracker and Wiki
 * @property summary [String] Mod summary
 * @property status [Int] Current mod status
 * @property downloadCount [integer(int64)] Number of downloads for the mod
 * @property isFeatured [Boolean] Whether the mod is included in the featured mods list
 * @property primaryCategoryId [integer(int32)] The main category of the mod as it was chosen by the mod author
 * @property categories [List<Category>] List of categories that this mod is related to
 * @property classId [integer(int32)¦null] The class id this mod belongs to
 * @property authors [List<ModAuthor>] List of the mod's authors
 * @property logo [ModAsset] The mod's logo asset
 * @property screenshots [List<ModAsset>] List of screenshots assets
 * @property mainFileId [integer(int32)] The id of the main file of the mod
 * @property latestFiles [List<File>] List of latest files of the mod
 * @property latestFilesIndexes [List<FileIndex>] List of file related details for the latest files of the mod
 * @property latestEarlyAccessFilesIndexes [List<FileIndex>] List of file related details for the latest early access files of the mod
 * @property dateCreated [String][(date-time)] The creation date of the mod
 * @property dateModified [String][(date-time)] The last time the mod was modified
 * @property dateReleased [String][(date-time)] The release date of the mod
 * @property allowModDistribution [boolean¦null] Is mod allowed to be distributed
 * @property gamePopularityRank [integer(int32)] The mod popularity rank for the game
 * @property isAvailable [Boolean] Is the mod available for search. This can be false when a mod is experimental, in a deleted state, or has only alpha files
 */
@Serializable
data class CfMod(
    val id: Int,
    val gameId: Int,
    val name: String,
    val slug: String,
    val links: ModLinks,
    val summary: String,
    val status: Int,
    val downloadCount: Int,
    val isFeatured: Boolean,
    val primaryCategoryId: Int,
    val categories: List<Category>,
    val classId: Int? = null,
    val authors: List<ModAuthor>,
    val logo: ModAsset,
    val screenshots: List<ModAsset> = listOf(),
    val mainFileId: Int,

    val latestFiles: List<File> = listOf(),
    val latestFilesIndexes: List<FileIndex> = listOf(),
    val latestEarlyAccessFilesIndexes: List<FileIndex> = listOf(),

    val dateCreated: String,
    val dateModified: String,
    val dateReleased: String,
    val allowModDistribution: Boolean? = null,
    val gamePopularityRank: Int,
    val isAvailable: Boolean,
) {
    @Serializable
    data class ModLinks(
        val websiteUrl: String,
        val wikiUrl: String? = null,
        val issuesUrl: String? = null,
        val sourceUrl: String? = null
    )

    @Serializable
    data class Category(
        val id: Int,
        val gameId: Int,
        val name: String,
        val slug: String,
        val url: String,
        val iconUrl: String,
        val dateModified: String,
        val isClass: Boolean? = null,
        val classId: Int? = null,
        val parentCategoryId: Int? = null,
        val displayIndex: Int? = null
    )

    @Serializable
    data class ModAuthor(
        val id: Int,
        val name: String,
        val url: String
    )

    @Serializable
    data class ModAsset(
        val id: Int,
        val modId: Int,
        val title: String,
        val description: String,
        val thumbnailUrl: String,
        val url: String
    )

    @Serializable
    data class File(
        val id: Int,
        val gameId: Int,
        val modId: Int,
        val isAvailable: Boolean,
        val displayName: String,
        val fileName: String,
        val releaseType: Int = 0,
        val fileStatus: Int = 0,
        val hashes: List<FileHash> = listOf(),
        val fileDate: String,
        val fileLength: Int,
        val downloadCount: Int,
        val fileSizeOnDisk: Int? = null,
        val downloadUrl: String? = null,
        val gameVersions: List<String> = listOf(),
        val sortableGameVersions: List<SortableGameVersion> = listOf(),
        val dependencies: List<FileDependency> = listOf(),
        val exposeAsAlternative: Boolean? = null,
        val parentProjectFileId: Int? = null,
        val alternateFileId: Int? = null,
        val isServerPack: Boolean? = null,
        val serverPackFileId: Int? = null,
        val isEarlyAccessContent: Boolean? = null,
        val earlyAccessEndDate: String? = null,
    ) {
        @Serializable
        data class FileHash(
            val value: String,
            val algo: Int
        )

        @Serializable
        data class SortableGameVersion(
            val gameVersionName: String,
            val gameVersionPadded: String,
            val gameVersion: String,
            val gameVersionReleaseDate: String,
            val gameVersionTypeId: Int? = null
        )

        @Serializable
        data class FileDependency(
            val modId: Int,
            val relationType: Int
        )
    }

    @Serializable
    data class FileIndex(
        val gameVersion: String,
        val fileId: Int,
        val filename: String,
        val releaseType: Int,
        val gameVersionTypeId: Int? = null,
        val modLoader: Int = 0
    )
}
