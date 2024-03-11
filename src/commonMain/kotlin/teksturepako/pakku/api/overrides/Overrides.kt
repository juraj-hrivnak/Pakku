package teksturepako.pakku.api.overrides

import korlibs.io.file.baseName
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.PAKKU_FOLDER
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.REAL
import teksturepako.pakku.api.projects.ProjectType

object Overrides
{
    fun filter(overrides: List<String>): List<String> = overrides.mapNotNull { overrideIn ->
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

    enum class ProjectOverrideLocation
    {
        PAKKU_FOLDER,
        REAL
    }

    @Serializable
    data class ProjectOverride(
        val type: ProjectType,
        @SerialName("file_name") val fileName: String,
        val location: ProjectOverrideLocation = PAKKU_FOLDER
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
        }.getOrDefault(mutableListOf())
    }

    suspend fun List<ProjectOverride>.toExportData(): Array<Pair<String, ByteArray>>
    {
        return this.map { projectOverride ->
            when (projectOverride.location)
            {
                PAKKU_FOLDER -> Pair(
                    "/overrides/${projectOverride.type.folderName}/${projectOverride.fileName}",
                    localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${projectOverride.type.folderName}/${projectOverride.fileName}"].readBytes()
                )
                REAL         -> Pair(
                    "/overrides/${projectOverride.type.folderName}/${projectOverride.fileName}",
                    localCurrentDirVfs["${projectOverride.type.folderName}/${projectOverride.fileName}"].readBytes()
                )
            }
        }.toTypedArray()
    }
}