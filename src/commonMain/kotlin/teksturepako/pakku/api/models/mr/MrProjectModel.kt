package teksturepako.pakku.api.models.mr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @property slug The slug of the project, used for vanity URLs. Regex: `^[\w!@$()`.+,"\-']{3,64}$`
 * @property title The title or name of the project
 * @property description A short description of the project
 * @property categories Array of strings representing a list of categories that the project belongs to
 * @property clientSide Enum representing client side support of the project: "required", "optional", "unsupported"
 * @property serverSide Enum representing server side support of the project: "required", "optional", "unsupported"
 * @property body A long form description of the project
 * @property status Enum representing the status of the project: "approved", "archived", "rejected", "draft",
 * "unlisted", "processing", "withheld", "scheduled", "private", "unknown"
 * @property requestedStatus Enum representing the requested status when submitting for review or scheduling
 * the project for release: "approved", "archived", "unlisted", "private", "draft"
 * @property additionalCategories Array of strings representing additional categories which are searchable but non-primary
 * @property issuesUrl An optional link to where to submit bugs or issues with the project
 * @property sourceUrl An optional link to the source code of the project
 * @property wikiUrl An optional link to the project's wiki page or other relevant information
 * @property discordUrl An optional invite link to the project's discord
 * @property donationUrls List of donation links for the project (Array of ProjectDonationURL objects)
 * @property projectType Enum representing the project type: "mod", "modpack", "resourcepack", "shader"
 * @property downloads The total number of downloads of the project
 * @property iconUrl The URL of the project's icon
 * @property color The RGB color of the project, automatically generated from the project icon
 * @property threadId The ID of the moderation thread associated with this project
 * @property monetizationStatus Enum representing the monetization status: "monetized", "demonetized", "force-demonetized"
 * @property id The ID of the project, encoded as a base62 string
 * @property team The ID of the team that has ownership of this project
 * @property published The date the project was published (string <ISO-8601>)
 * @property updated The date the project was last updated (string <ISO-8601>)
 * @property approved The date the project's status was set to an approved status (string or null <ISO-8601>)
 * @property queued The date the project's status was submitted to moderators for review (string or null <ISO-8601>)
 * @property followers The total number of users following the project
 * @property license The license of the project (object ProjectLicense)
 * @property versions Array of strings representing version IDs of the project (will never be empty unless draft status)
 * @property gameVersions Array of strings representing all game versions supported by the project
 * @property loaders Array of strings representing all loaders supported by the project
 * @property gallery Array of objects or null representing images uploaded to the project's gallery (Array of GalleryImage objects)
 */
@Serializable
data class MrProjectModel(
    val slug: String,
    val title: String,
    val description: String,
    val categories: List<String> = listOf(),
    @SerialName("client_side") val clientSide: String,
    @SerialName("server_side") val serverSide: String,
    val body: String,
    val status: String,
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("additional_categories") val additionalCategories: List<String> = listOf(),
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    @SerialName("donation_urls") val donationUrls: List<DonationUrl> = listOf(),
    @SerialName("project_type") val projectType: String,
    val downloads: Int,
    @SerialName("icon_url") val iconUrl: String? = null,
    val color: Int? = null,
    @SerialName("thread_id") val threadId: String,
    @SerialName("monetization_status") val monetizationStatus: String,
    val id: String,
    val team: String,

    val published: String,
    val updated: String,
    val approved: String? = null,
    val queued: String? = null,
    val followers: Int,
    val license: License,
    val versions: List<String> = listOf(),
    @SerialName("game_versions") val gameVersions: List<String> = listOf(),
    val loaders: List<String> = listOf(),
    val gallery: List<GalleryImage>? = null
) {
    /**
     * @property id The ID of the donation platform
     * @property platform The donation platform this link is associated with
     * @property url The URL of the donation platform and user
     */
    @Serializable
    data class DonationUrl(
        val id: String,
        val platform: String,
        val url: String
    )

    /**
     * @property id The SPDX license ID of the project
     * @property name The long name of the license
     * @property url The URL to this license (nullable)
     */
    @Serializable
    data class License(
        val id: String,
        val name: String,
        val url: String? = null
    )

    /**
     * @property url The URL of the gallery image
     * @property featured Whether the image is featured in the gallery
     * @property title The title of the gallery image (nullable)
     * @property description The description of the gallery image (nullable)
     * @property created The date and time the gallery image was created (string <ISO-8601>)
     * @property ordering The order of the gallery image. Gallery images are sorted by this field and then alphabetically by title.
     */
    @Serializable
    data class GalleryImage(
        val url: String,
        val featured: Boolean,
        val title: String? = null,
        val description: String? = null,
        val created: String,
        val ordering: Int
    )
}