package teksturepako.pakku.api.overrides

import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug
import teksturepako.pakku.io.readPathBytesOrNull
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
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
        suspend fun createOrNull(path: Path): ProjectOverride?
        {
            debug { println(path) }

            if (path.notExists() && !path.isRegularFile()) return null

            val projectType = ProjectType.entries.firstOrNull {
                it.folderName == path.parent.name
            } ?: return null

            val type = OverrideType.entries.firstOrNull {
                it.folderName == path.parent.parent.name
            } ?: return null

            val bytes = readPathBytesOrNull(path) ?: return null

            return ProjectOverride(
                type,
                path,
                Path(workingPath, projectType.folderName, path.name),
                Path(projectType.folderName, path.name),
                bytes
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
