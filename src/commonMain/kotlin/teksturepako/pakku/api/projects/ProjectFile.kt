package teksturepako.pakku.api.projects

import kotlinx.datetime.Instant
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.HashMismatch
import teksturepako.pakku.api.actions.ActionError.NoHashes
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.io.createHash
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
@SerialName("project_file")
data class ProjectFile(
    @Required val type: String,
    @SerialName("file_name") val fileName: String = "",
    @SerialName("mc_versions") val mcVersions: MutableList<String> = mutableListOf(),
    val loaders: MutableList<String> = mutableListOf(),
    @SerialName("release_type") val releaseType: String = "",
    var url: String? = null,
    val id: String = "",
    @SerialName("parent_id") val parentId: String = "",
    val hashes: MutableMap<String, String>? = null,
    @SerialName("required_dependencies") val requiredDependencies: MutableSet<String>? = null,
    val size: Int = 0,
    @SerialName("date_published") val datePublished: Instant = Instant.DISTANT_PAST,
)
{
    // -- PARENT --

    fun getParentProject(lockFile: LockFile): Project? = lockFile.getProject(parentId)

    // -- FILE PATH --

    fun getPath(parentProject: Project, configFile: ConfigFile?): Path
    {
        val parentPathString = parentProject.getPathStringWithSubpath(configFile)
        return Path(workingPath, parentPathString, fileName)
    }

    fun getRelativePathString(parentProject: Project, configFile: ConfigFile?, separator: Char = '/'): String
    {
        val parentPathString = parentProject.getPathStringWithSubpath(configFile, separator)
        return "$parentPathString$separator$fileName"
    }

    // -- INTEGRITY --

    fun checkIntegrity(bytes: ByteArray, path: Path): ActionError?
    {
        if (this.hashes == null) return NoHashes(path)

        for ((hashType, originalHash) in this.hashes)
        {
            val newHash = createHash(hashType, bytes)

            if (originalHash != newHash)
            {
                return HashMismatch(path, originalHash, newHash)
            }
            else continue
        }

        return null
    }
}