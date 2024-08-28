package teksturepako.pakku.api.actions

import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

data class RequestHandlers(
    val onError: suspend (error: ActionError) -> Unit,
    val onSuccess: suspend (project: Project, isRecommended: Boolean, ctx: RequestHandlers) -> Unit
)

suspend fun Project?.createAdditionRequest(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (project: Project, isRecommended: Boolean, ctx: RequestHandlers) -> Unit,
    lockFile: LockFile,
    platforms: List<Platform>,
    strict: Boolean = false
)
{
    // Exist
    val project = this ?: return onError(ProjNotFound())
    var isRecommended = true

    // Already added
    if (lockFile.isProjectAdded(project))
    {
        return onError(AlreadyAdded(project))
    }

    for (platform in platforms)
    {
        // Check if project is on each platform
        if (project.isNotOnPlatform(platform))
        {
            if (!strict) continue
            else
            {
                onError(NotFoundOnPlatform(project, platform))
                return
            }
        }

        // Check if project has files on each platform
        if (project.hasNoFilesOnPlatform(platform))
        {
            onError(NoFilesOnPlatform(project, platform))
            isRecommended = false
        }
    }

    // Check if project has any files at all
    if (project.hasNoFiles())
    {
        return onError(NoFiles(project, lockFile))
    }

    // Check if project files match across platforms
    if (project.fileNamesDoNotMatchAcrossPlatforms(platforms))
    {
        onError(FileNamesDoNotMatch(project))
        isRecommended = false
    }

    onSuccess(project, isRecommended, RequestHandlers(onError, onSuccess))
}
