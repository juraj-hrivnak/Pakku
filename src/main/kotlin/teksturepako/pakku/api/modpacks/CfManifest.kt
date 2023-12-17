package teksturepako.pakku.api.modpacks

import kotlinx.serialization.Serializable

@Serializable
data class CfManifest(
    val minecraft: CfMinecraftData,
    val manifestType: String,
    val manifestVersion: Double,
    val name: String,
    val version: String,
    val author: String,
    val files: List<CfModData>,
    val overrides: String
)

@Serializable
data class CfMinecraftData(
    val version: String,
    val modLoaders: List<CfModLoaderData>
)

@Serializable
data class CfModLoaderData(
    val id: String,
    val primary: Boolean
)

@Serializable
data class CfModData(
    val projectID: String,
    val fileID: String,
    val required: Boolean
)