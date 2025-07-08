package teksturepako.pakku.api.actions

import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

data class RequestHandlers(
    val onError: suspend (error: ActionError) -> Unit,
    val onSuccess: suspend (
        project: Project, isRecommended: Boolean, replacing: Project?, reqHandlers: RequestHandlers
    ) -> Unit
)

suspend fun Project?.createAdditionRequest(
    onError: suspend (error: ActionError) -> Unit,
    onSuccess: suspend (
        project: Project, isRecommended: Boolean, replacing: Project?, reqHandlers: RequestHandlers
    ) -> Unit,
    lockFile: LockFile,
    platforms: List<Platform>,
    strict: Boolean = false
)
{
    // Exist
    if (this == null) return onError(ProjNotFound())

    var isRecommended = true

    // Handle already added project
    val replacing = if (lockFile.isProjectAdded(this))
    {
        val existingProject = lockFile.getProject(this) ?: return onError(ProjNotFound())

        onError(AlreadyAdded(existingProject))
        if (existingProject.files == this.files) return else existingProject
    }
    else null

    // We do not have to check platform for GitHub only project
    if (this.slug.keys.size > 1 || this.slug.keys.firstOrNull() != GitHub.serialName)
    {
        for (platform in platforms)
        {
            // Check if project is on each platform
            if (this.isNotOnPlatform(platform))
            {
                if (!strict) continue else return onError(NotFoundOn(this, platform))
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
    if (this.versionsDoNotMatchAcrossProviders(platforms))
    {
        onError(VersionsDoNotMatch(this))
        isRecommended = false
    }

    onSuccess(this, isRecommended, replacing, RequestHandlers(onError, onSuccess))
}
