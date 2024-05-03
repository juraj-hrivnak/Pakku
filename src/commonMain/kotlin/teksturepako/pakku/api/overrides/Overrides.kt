package teksturepako.pakku.api.overrides

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.FAKE
import teksturepako.pakku.api.overrides.Overrides.ProjectOverrideLocation.REAL
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
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
        FAKE, REAL
    }

    @Serializable
    data class ProjectOverride(
        @SerialName("project_type") val projectType: ProjectType,
        @SerialName("override_type") val overrideType: OverrideType,
        @SerialName("file_name") val fileName: String,
        val location: ProjectOverrideLocation = FAKE
    )

    const val PAKKU_DIR = ".pakku"

    fun getProjectOverrides(): List<ProjectOverride>
    {
        return runCatching {
            OverrideType.entries.flatMap { overrideType ->
                ProjectType.entries.flatMap { projectType ->
                    File("$workingPath/$PAKKU_DIR/${overrideType.folderName}/${projectType.folderName}")
                        .walkTopDown()
                        .mapNotNull {
                            if (it.isFile) ProjectOverride(projectType, overrideType, it.name) else null
                        }
                }
            }
        }.getOrDefault(mutableListOf())
    }

    fun Project.toOverrideType(): OverrideType = when
    {
        this.type == ProjectType.SHADER || this.type == ProjectType.RESOURCE_PACK -> OverrideType.CLIENT_OVERRIDE
        this.side == ProjectSide.BOTH                                             -> OverrideType.OVERRIDE
        this.side == ProjectSide.SERVER                                           -> OverrideType.SERVER_OVERRIDE
        this.side == ProjectSide.CLIENT                                           -> OverrideType.CLIENT_OVERRIDE
        this.side == null                                                         -> OverrideType.OVERRIDE
        else                                                                      -> OverrideType.OVERRIDE
    }

    suspend fun List<ProjectOverride>.toExportData(
        noOverrideFolders: Boolean = false,
        onlyOverridesFolders: Boolean = false
    ): List<Result<Pair<String, ByteArray>>>
    {
        return this.map { projectOverride ->
            val resultOverridesFolder = when
            {
                noOverrideFolders    -> ""
                onlyOverridesFolders -> "overrides"
                else                 -> projectOverride.overrideType.folderName
            }
            when (projectOverride.location)
            {
                FAKE ->
                {
                    val bytes = readFileBytesOrNull(
                        "$workingPath/$PAKKU_DIR/${projectOverride.overrideType.folderName}/" +
                                "${projectOverride.projectType.folderName}/${projectOverride.fileName}"
                    )

                    if (bytes != null)
                    {
                        Result.success("$resultOverridesFolder/" +
                                "${projectOverride.projectType.folderName}/${projectOverride.fileName}" to bytes)
                    }
                    else
                    {
                        Result.failure(PakkuException(
                            "Project overrides '${projectOverride.fileName}' could not be found.\n" +
                                    " Try running the 'pakku fetch' command before exporting the modpack."
                        ))
                    }
                }

                REAL         ->
                {
                    val bytes = readFileBytesOrNull(
                        "$workingPath/${projectOverride.projectType.folderName}/${projectOverride.fileName}"
                    )

                    if (bytes != null)
                    {
                        Result.success("$resultOverridesFolder/" +
                                "${projectOverride.projectType.folderName}/${projectOverride.fileName}" to bytes)
                    }
                    else
                    {
                        Result.failure(PakkuException(
                            "Project overrides '${projectOverride.fileName}' could not be found.\n" +
                                    " Try running the 'pakku fetch' command before exporting the modpack."
                        ))
                    }
                }
            }
        }
    }

}