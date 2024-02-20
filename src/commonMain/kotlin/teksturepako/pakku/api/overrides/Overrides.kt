package teksturepako.pakku.api.overrides

import korlibs.io.file.baseName
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.SerialName
import teksturepako.pakku.api.projects.ProjectType

object Overrides
{
    fun filter(overrides: List<String>): List<String>
    {
        return overrides.mapNotNull { overrideIn ->
            val override = runCatching { localCurrentDirVfs[overrideIn] }.getOrNull()

            if (override == null) return@mapNotNull null

            val path = override.relativePathTo(localCurrentDirVfs["."])!!

            if (path.contains("..")
                || path.contains("[A-Z]:/".toRegex())
                || path.contains("[A-Z]:\\\\".toRegex())
                || path.startsWith("/")
                || path.startsWith("\\\\")
            ) return@mapNotNull null

            return@mapNotNull path
        }
    }

    data class ProjectOverride(
        val type: ProjectType,
        @SerialName("file_name") val fileName: String = "",
    )

    const val PROJECT_OVERRIDES_FOLDER = ".pakku/overrides"

    suspend fun getProjectOverrides(): List<ProjectOverride>
    {
        return runCatching {
            ProjectType.entries.flatMap { projectType ->
                localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${projectType.folderName}"].listRecursiveSimple().map {
                    ProjectOverride(projectType, it.baseName)
                }
            }
        }.getOrDefault(listOf())
    }
}