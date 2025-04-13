package teksturepako.pakku.api.overrides

import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType

@Suppress("unused")
enum class OverrideType(val prettyName: String, val folderName: String)
{
    OVERRIDE("override", "overrides"),
    SERVER_OVERRIDE("server override", "server-overrides"),
    CLIENT_OVERRIDE("client override", "client-overrides");

    companion object
    {
        fun fromProject(project: Project): OverrideType = when (project.type)
        {
            ProjectType.MOD ->
            {
                when (project.side)
                {
                    ProjectSide.CLIENT -> CLIENT_OVERRIDE
                    ProjectSide.SERVER -> SERVER_OVERRIDE
                    ProjectSide.BOTH   -> OVERRIDE
                    null               -> OVERRIDE
                }
            }
            ProjectType.RESOURCE_PACK  -> CLIENT_OVERRIDE
            ProjectType.DATA_PACK      ->
            {
                when (project.side)
                {
                    ProjectSide.CLIENT -> CLIENT_OVERRIDE
                    ProjectSide.SERVER -> SERVER_OVERRIDE
                    ProjectSide.BOTH   -> OVERRIDE
                    null               -> OVERRIDE
                }
            }
            ProjectType.WORLD          -> SERVER_OVERRIDE
            ProjectType.SHADER         -> CLIENT_OVERRIDE
        }
    }
}