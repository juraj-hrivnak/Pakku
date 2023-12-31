package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

fun Project?.createAdditionRequest(
    onError: ErrorBlock,
    onRetry: RetryBlock,
    onSuccess: SuccessBlock,
    pakkuLock: PakkuLock,
    platforms: List<Platform>
)
{
    // Exist
    var project = this ?: return onError.error(Error.ProjectNotFound("Project not found"))
    var isRecommended = true

    // Already added
    if (pakkuLock.isProjectAdded(project))
    {
        return onError.error(Error.CouldNotAdd("Could not add ${project.slug}. It is already added"))
    }

    for (platform in platforms)
    {
        // Check if project is on each platform
        if (project.isNotOnPlatform(platform))
        {
            onError.error(Error.NotFoundOnPlatform("${project.slug} was not found on ${platform.name}"))

            // Retry
            val project2 =  onRetry.retryWith(platform)
            if (project2 != null && project2.hasFilesOnPlatform(platform)) project += project2
            continue
        }

        // Check if project has files on each platform
        if (project.hasNoFilesOnPlatform(platform))
        {
            onError.error(Error.NoFilesOnPlatform("No files for ${project.slug} found on ${platform.name}"))
            isRecommended = false
        }
    }

    // Check if project has any files at all
    if (project.hasNoFiles())
    {
        onError.error(Error.NoFiles("No files found for ${project.slug} ${pakkuLock.getMcVersions()}"))
        isRecommended = false
    }

    // Check if project files match across platforms
    if (project.fileNamesDoNotMatchAcrossPlatforms(platforms))
    {
        onError.error(Error.FileNamesDoNotMatch("${project.slug} versions do not match across platforms"))
        onError.error(Error.FileNamesDoNotMatch((json.encodeToString(project.files.map { it.fileName }))))
        isRecommended = false
    }

    onSuccess.success(project, isRecommended, RequestHandlers(onError, onRetry, onSuccess))
}

data class RequestHandlers(
    val onError: ErrorBlock,
    val onRetry: RetryBlock,
    val onSuccess: SuccessBlock
)

sealed class Error(val message: String)
{
    class ProjectNotFound(message: String) : Error(message)
    class CouldNotAdd(message: String) : Error(message)
    class NotFoundOnPlatform(message: String) : Error(message)
    class NoFilesOnPlatform(message: String) : Error(message)
    class NoFiles(message: String) : Error(message)
    class FileNamesDoNotMatch(message: String) : Error(message)
}

fun interface ErrorBlock
{
    fun error(error: Error)
}

fun interface RetryBlock
{
    fun retryWith(platform: Platform): Project?
}

fun interface SuccessBlock
{
    fun success(project: Project, isRecommended: Boolean, ctx: RequestHandlers)
}