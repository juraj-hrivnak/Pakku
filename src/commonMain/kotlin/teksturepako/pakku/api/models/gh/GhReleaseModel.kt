package teksturepako.pakku.api.models.gh


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhReleaseModel(
    val url: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets_url") val assetsUrl: String,
    @SerialName("upload_url") val uploadUrl: String,
    @SerialName("tarball_url") val tarballUrl: String,
    @SerialName("zipball_url") val zipballUrl: String,
    val id: Int,
    @SerialName("node_id") val nodeId: String,
    @SerialName("tag_name") val tagName: String,
    @SerialName("target_commitish") val targetCommitish: String,
    val name: String,
    val body: String,
    val draft: Boolean,
    val prerelease: Boolean,
    @SerialName("created_at") val createdAt: String, // date-time
    @SerialName("published_at") val publishedAt: String, // date-time
    val author: Author,
    val assets: List<Asset> = listOf(),
)
{
    @Serializable
    data class Author(
        val login: String,
        val id: Int,
        @SerialName("node_id") val nodeId: String,
        @SerialName("avatar_url") val avatarUrl: String,
        @SerialName("gravatar_id") val gravatarId: String,
        val url: String,
        @SerialName("html_url") val htmlUrl: String,
        @SerialName("followers_url") val followersUrl: String,
        @SerialName("following_url") val followingUrl: String,
        @SerialName("gists_url") val gistsUrl: String,
        @SerialName("starred_url") val starredUrl: String,
        @SerialName("subscriptions_url") val subscriptionsUrl: String,
        @SerialName("organizations_url") val organizationsUrl: String,
        @SerialName("repos_url") val reposUrl: String,
        @SerialName("events_url") val eventsUrl: String,
        @SerialName("received_events_url") val receivedEventsUrl: String,
        val type: String,
        @SerialName("site_admin") val siteAdmin: Boolean,
    )

    @Serializable
    data class Asset(
        val url: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
        val id: Int,
        @SerialName("node_id") val nodeId: String,
        /** The file name of the asset. */
        val name: String,
        val label: String,
        /** State of the release asset. */
        val state: String,
        @SerialName("content_type") val contentType: String,
        val size: Int,
        @SerialName("download_count") val downloadCount: Int,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        val uploader: Author
    )
}
