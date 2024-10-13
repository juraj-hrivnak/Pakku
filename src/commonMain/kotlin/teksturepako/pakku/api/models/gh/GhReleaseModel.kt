package teksturepako.pakku.api.models.gh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhReleaseModel(
    val url: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets_url") val assetsUrl: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("tarball_url") val tarballUrl: String? = null,
    @SerialName("zipball_url") val zipballUrl: String? = null,
    val id: Int,
    @SerialName("node_id") val nodeId: String,
    @SerialName("tag_name") val tagName: String,
    @SerialName("target_commitish") val targetCommitish: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at") val createdAt: String, // date-time
    @SerialName("published_at") val publishedAt: String? = null, // date-time
    val author: GhOwnerModel,
    val assets: List<Asset> = listOf(),
)
{
    @Serializable
    data class Asset(
        val url: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
        val id: Int,
        @SerialName("node_id") val nodeId: String,
        /** The file name of the asset. */
        val name: String,
        val label: String? = null,
        /** State of the release asset. */
        val state: String,
        @SerialName("content_type") val contentType: String,
        val size: Int,
        @SerialName("download_count") val downloadCount: Int,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        val uploader: GhOwnerModel? = null
    )
}
