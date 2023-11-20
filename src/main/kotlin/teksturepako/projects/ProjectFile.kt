package teksturepako.projects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ProjectFile(
    @SerialName("file_name")
    val fileName: String,

    @SerialName("mc_version")
    val mcVersion: String,

    var url: String? = null,

    @Transient
    val data: PlatformFileData? = null,
)

abstract class PlatformFileData {
    abstract val data: Any
}

@Serializable
data class MrFile(val hashes: MutableMap<String, String> = mutableMapOf()) : PlatformFileData()
{
    override val data = hashes
}

@Serializable
data class CfFile(val id: Int) : PlatformFileData()
{
    override val data = id
}