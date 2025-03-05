package teksturepako.pakku.api.models

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

interface ModpackModel
{
    /** Requests project found it this [ModpackModel]. */
    suspend fun toSetOfProjects(
        lockFile: LockFile, platforms: List<Platform>
    ): Result<Set<Project>, ActionError>

    /** Converts [ModpackModel] to [LockFile]. */
    suspend fun toLockFile(): LockFile
}
