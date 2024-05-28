package teksturepako.pakku.api.actions.import

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.CouldNotImport
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

suspend fun import(
    path: String,
    lockFile: LockFile,
    platforms: List<Platform>
): Result<Set<Project>, ActionError>
{
    val modpack = when
    {
        path.isCfModpack() -> importCurseForge(path)
        path.isMrModpack() -> importModrinth(path)
        else               -> Err(CouldNotImport(path))
    }

    return modpack.map { it.toSetOfProjects(lockFile, platforms) }
}