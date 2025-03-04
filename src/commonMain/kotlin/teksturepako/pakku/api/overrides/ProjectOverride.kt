package teksturepako.pakku.api.overrides

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.readPathBytesOrNull
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.notExists

data class ProjectOverride(
    val type: OverrideType,
    val path: Path,
    val fullOutputPath: Path,
    val relativeOutputPath: Path,
    val bytes: ByteArray
)
{
    companion object
    {
        suspend fun fromPath(path: Path, configFile: ConfigFile?): ProjectOverride?
        {
            val separator = File.separator

            if (path.notExists() && !path.isRegularFile()) return null

            val type: OverrideType = OverrideType.entries.firstOrNull { overrideType ->
                "$separator${overrideType.folderName}$separator" in path.absolutePathString()
            } ?: return null

            val projectType = ProjectType.entries.firstOrNull { projectType ->
                "$separator${projectType.getPathString(configFile)}$separator" in path.absolutePathString()
                    .substringAfter(type.folderName)
            } ?: return null

            val bytes = readPathBytesOrNull(path) ?: return null

            val relativePath = path.absolutePathString().substringAfter(
                "${Dirs.PAKKU_DIR}$separator${type.folderName}$separator${projectType.getPathString(configFile)}$separator",
                ""
            )

            if (relativePath.isBlank()) return null

            val prjTypePathString = projectType.getPathString(configFile)

            return ProjectOverride(
                type = type,
                path = path,
                fullOutputPath = Path(workingPath, prjTypePathString, relativePath),
                relativeOutputPath = Path(prjTypePathString, relativePath),
                bytes = bytes
            )
        }
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectOverride

        if (type != other.type) return false
        if (path != other.path) return false
        if (fullOutputPath != other.fullOutputPath) return false
        if (relativeOutputPath != other.relativeOutputPath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int
    {
        var result = type.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + fullOutputPath.hashCode()
        result = 31 * result + relativeOutputPath.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
