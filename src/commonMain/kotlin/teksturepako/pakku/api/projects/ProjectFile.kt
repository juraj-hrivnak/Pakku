package teksturepako.pakku.api.projects

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.HashFailed
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
)
{
    // -- PARENT --

    fun getParentProject(lockFile: LockFile): Project? = lockFile.getProject(parentId)

    // -- FILE PATH --

    @Transient
    private lateinit var path: Path

    fun getPath(lockFile: LockFile): Path?
    {
        val parentProject = getParentProject(lockFile) ?: return null

        val path = Path(workingPath, parentProject.type.folderName, fileName)
        this.path = path

        return path
    }

    fun getPath(): Path? = if (!this::path.isInitialized) null else this.path

    // -- INTEGRITY --

    fun checkIntegrity(bytes: ByteArray): Result<ByteArray, ActionError>
    {
        if (this.hashes == null) return Err(ActionError.NoHashes(this))

        for ((hashType, originalHash) in this.hashes)
        {
            val newHash = createHash(hashType, bytes)

            if (originalHash != newHash)
            {
                return Err(HashFailed(this, originalHash, newHash))
            }
            else continue
        }

        return Ok(bytes)
    }
}