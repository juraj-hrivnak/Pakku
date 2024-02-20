package teksturepako.pakku.api.models

import kotlinx.serialization.Serializable

@Serializable
data class MrModpackModel(
    val formatVersion: Int = 1,
    val game: String = "minecraft",
    val versionId: String = "",
    val name: String = "",
    val files: Set<File> = setOf(),
    val dependencies: Map<String, String> = mapOf()
)
{
    @Serializable
    data class File(
        val path: String,
        val hashes: Hashes,
        val env: Env? = null,
        val downloads: Set<String>,
        val fileSize: Int
    ) {
        @Serializable
        data class Hashes(
            val sha512: String,
            val sha1: String
        )

        @Serializable
        data class Env(
            val client: String = "required",
            val server: String = "required"
        )
    }
}
