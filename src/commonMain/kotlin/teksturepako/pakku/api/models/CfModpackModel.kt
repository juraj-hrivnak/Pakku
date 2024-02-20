package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable

@Serializable
data class CfModpackModel(
    val minecraft: CfMinecraftData,
    val manifestType: String = "minecraftModpack",
    val manifestVersion: Int = 1,
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val files: List<CfModData>,
    val overrides: String = "overrides"
) {
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
        val required: Boolean = true
    )
}