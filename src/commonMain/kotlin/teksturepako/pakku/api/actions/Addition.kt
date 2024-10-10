package teksturepako.pakku.api.actions

import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
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
    val project = this?.copy(files = this.files.toMutableSet()) ?: return onError(ProjNotFound())
    var isRecommended = true

    // Already added
    val existingProject = lockFile.getProject(this)
    if (existingProject != null)
    {
        project.files.removeAll(existingProject.files)
        if (project.hasNoFiles()) return onError(AlreadyAdded(this))
    }

    // We do not have to check platform for GitHub only project
    if (this.slug.keys.size > 1 || this.slug.keys.firstOrNull() != GitHub.serialName)
    {
        for (platform in platforms)
        {
            // Check if project is on each platform
            if (this.isNotOnPlatform(platform))
            {
                if (!strict) continue
                else
                {
                    onError(NotFoundOn(this, platform))
                    return
                }
            }

            // Check if project has files on each platform
            if (this.hasNoFilesOnPlatform(platform))
            {
                onError(NoFilesOn(this, platform))
                isRecommended = false
            }
        }
    }


    // Check if project has any files at all
    if (this.hasNoFiles())
    {
        return onError(NoFiles(this, lockFile))
    }

    // Check if project files match across platforms
    if (this.fileNamesDoNotMatchAcrossPlatforms(platforms))
    {
        onError(FileNamesDoNotMatch(this))
        isRecommended = false
    }

    onSuccess(this, isRecommended, RequestHandlers(onError, onSuccess))
}
