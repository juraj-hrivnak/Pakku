package teksturepako.pakku.api.models.mmc

import kotlinx.serialization.Serializable

@Serializable
data class MultiMcPackModel(
    val components: List<Component> = listOf(),
    val formatVersion: Int = 1
) {
    @Serializable
    data class Component(
        val important: Boolean? = null,
        val dependencyOnly: Boolean? = null,
        val uid: String,
        val version: String,
    )
}