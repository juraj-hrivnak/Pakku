package teksturepako.pakku.api.models

import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

interface ModpackModel
{
    suspend fun toSetOfProjects(
        lockFile: LockFile, platforms: List<Platform>
    ): Set<Project>

    suspend fun toLockFile(): LockFile
}
