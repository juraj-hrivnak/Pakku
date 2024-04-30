package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.PError.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

data class RequestHandlers(
    val onError: suspend (error: PError) -> Unit,
    val onRetry: suspend (platform: Platform, project: Project) -> Project?,
    val onSuccess: suspend (project: Project, isRecommended: Boolean, ctx: RequestHandlers) -> Unit
)

suspend fun Project?.createAdditionRequest(
    onError: suspend (error: PError) -> Unit,
    onRetry: suspend (platform: Platform, project: Project) -> Project?,
    onSuccess: suspend (project: Project, isRecommended: Boolean, ctx: RequestHandlers) -> Unit,
    lockFile: LockFile,
    platforms: List<Platform>
)
{
    // Exist
    var project = this ?: return onError(ProjNotFound("Project not found"))
    var isRecommended = true

    // Already added
    if (lockFile.isProjectAdded(project))
    {
        return onError(AlreadyAdded("Could not add ${project.slug}. It is already added"))
    }

    for (platform in platforms)
    {
        // Check if project is on each platform
        if (project.isNotOnPlatform(platform))
        {
            onError(NotFoundOnPlatform("${project.slug} was not found on ${platform.name}"))

            debug { println(project.toPrettyString()) }

            // Retry
            val project2 =  onRetry(platform, project)
            if (project2 != null && project2.hasFilesOnPlatform(platform)) project += project2
            debug { println(project2?.toPrettyString()) }
            continue
        }

        // Check if project has files on each platform
        if (project.hasNoFilesOnPlatform(platform))
        {
            onError(NoFilesOnPlatform("No files for ${project.slug} found on ${platform.name}"))
            isRecommended = false
        }
    }

    // Check if project has any files at all
    if (project.hasNoFiles())
    {
        onError(NoFiles("No files found for ${project.slug} ${lockFile.getMcVersions()}"))
        isRecommended = false
    }

    // Check if project files match across platforms
    if (project.fileNamesDoNotMatchAcrossPlatforms(platforms))
    {
        onError(FileNamesDoNotMatch("${project.slug} versions do not match across platforms"))
        onError(FileNamesDoNotMatch((json.encodeToString(project.files.map { it.fileName }))))
        isRecommended = false
    }

    onSuccess(project, isRecommended, RequestHandlers(onError, onRetry, onSuccess))
}
