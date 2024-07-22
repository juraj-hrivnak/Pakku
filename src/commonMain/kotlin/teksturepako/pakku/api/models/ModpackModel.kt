package teksturepako.pakku.api.models

import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

interface ModpackModel
{
    /** Requests project found it this [ModpackModel]. */
    suspend fun toSetOfProjects(
        lockFile: LockFile, platforms: List<Platform>
    ): Set<Project>

    /** Converts [ModpackModel] to [LockFile]. */
    suspend fun toLockFile(): LockFile
}
