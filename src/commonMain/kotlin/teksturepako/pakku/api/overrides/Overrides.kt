package teksturepako.pakku.api.overrides

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.PAKKU_FOLDER
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.REAL
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.filterPath
import teksturepako.pakku.io.readFileBytesOrNull
import java.io.File

object Overrides
{
    fun filter(overrides: List<String>): List<String> = overrides.mapNotNull { overrideIn ->
        val override = runCatching { File(overrideIn) }.getOrNull()

        return@mapNotNull if (override == null) null else
        {
            filterPath(override.path)
        }
    }

    enum class ProjectOverrideLocation
    {
        PAKKU_FOLDER, REAL
    }

    @Serializable
    data class ProjectOverride(
        val type: ProjectType,
        @SerialName("file_name") val fileName: String,
        val location: ProjectOverrideLocation = PAKKU_FOLDER
    )

    const val PROJECT_OVERRIDES_FOLDER = ".pakku/overrides"

    fun getProjectOverrides(): List<ProjectOverride>
    {
        return runCatching {
            ProjectType.entries.flatMap { projectType ->
                File("$PROJECT_OVERRIDES_FOLDER/${projectType.folderName}").walkTopDown().mapNotNull {
                    if (it.isFile) ProjectOverride(projectType, it.name)
                    else null
                }
            }
        }.getOrDefault(mutableListOf())
    }

    suspend fun List<ProjectOverride>.toExportData(
        resultLocation: String = "/overrides/"
    ): List<Result<Pair<String, ByteArray>>> = this.map {
        projectOverride ->
            when (projectOverride.location)
            {
                PAKKU_FOLDER ->
                {
                    val bytes =
                        readFileBytesOrNull("$PROJECT_OVERRIDES_FOLDER/${projectOverride.type.folderName}/${projectOverride.fileName}")

                    if (bytes != null) Result.success("$resultLocation${projectOverride.type
                        .folderName}/${projectOverride.fileName}" to bytes)
                    else Result.failure(PakkuException(
                        "Project overrides '${projectOverride.fileName}' could not be found.\n" +
                                " Try running the 'pakku fetch' command before exporting the modpack."
                    ))
                }

                REAL         ->
                {
                    val bytes =
                        readFileBytesOrNull("${projectOverride.type.folderName}/${projectOverride.fileName}")

                    if (bytes != null) Result.success("$resultLocation${projectOverride.type
                        .folderName}/${projectOverride.fileName}" to bytes)
                    else Result.failure(PakkuException(
                        "Project overrides '${projectOverride.fileName}' could not be found.\n" +
                                " Try running the 'pakku fetch' command before exporting the modpack."
                    ))
                }
            }
        }

}