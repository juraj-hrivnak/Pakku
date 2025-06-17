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

data class ManualOverride(
    val type: OverrideType,
    val path: Path,
    val fullOutputPath: Path,
    val relativeOutputPath: Path,
    val isInPrimaryDirectory: Boolean
)
{
    companion object
    {
        fun fromPath(path: Path, configFile: ConfigFile?): ManualOverride?
        {
            val separator = File.separator

            if (path.notExists() && !path.isRegularFile()) return null

            val type: OverrideType = OverrideType.entries.firstOrNull { overrideType ->
                "$separator${overrideType.folderName}$separator" in path.absolutePathString()
            } ?: return null

            val projectType = ProjectType.entries.firstOrNull { projectType ->
                "$separator${projectType.getPathString(configFile)}$separator" in path.absolutePathString()
                    .substringAfter(type.folderName)
            }

            val relativePath = if (projectType != null)
            {
                path.absolutePathString().substringAfter(
                    "${Dirs.PAKKU_DIR}$separator${type.folderName}$separator${projectType.getPathString(configFile)}$separator",
                    ""
                )
            }
            else
            {
                path.absolutePathString().substringAfter(
                    "${Dirs.PAKKU_DIR}$separator${type.folderName}$separator", ""
                )
            }

            if (relativePath.isBlank()) return null

            val prjTypePathString = projectType?.getPathString(configFile)

            return if (prjTypePathString != null)
            {
                ManualOverride(
                    type = type,
                    path = path,
                    fullOutputPath = Path(workingPath, prjTypePathString, relativePath),
                    relativeOutputPath = Path(prjTypePathString, relativePath),
                    isInPrimaryDirectory = false
                )
            }
            else
            {
                ManualOverride(
                    type = type,
                    path = path,
                    fullOutputPath = Path(workingPath, relativePath),
                    relativeOutputPath = Path(relativePath),
                    isInPrimaryDirectory = true
                )
            }
        }
    }
}
