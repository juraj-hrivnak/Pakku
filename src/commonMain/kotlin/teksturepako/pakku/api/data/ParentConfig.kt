package teksturepako.pakku.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for tracking a parent modpack.
 * Enables fork management where a modpack can sync updates from an upstream source.
 *
 * @property type The platform type: "curseforge", "modrinth", or "local"
 * @property id The project identifier (numeric ID for CurseForge, slug for Modrinth, path for local)
 * @property version The version to track. `null` means track latest, specific string pins to that version
 * @property autoSync Whether to automatically sync with parent on updates (currently unused, reserved for future)
 */
@Serializable
data class ParentConfig(
    @SerialName("type") val type: String,
    @SerialName("id") val id: String,
    @SerialName("version") val version: String? = null,
    @SerialName("autoSync") val autoSync: Boolean = false
)
