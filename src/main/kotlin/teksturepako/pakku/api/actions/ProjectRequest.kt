package teksturepako.pakku.api.actions

import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

fun Project?.createRequest(
    onError: ErrorBlock,
    onRetry: RetryBlock,
    onSuccess: SuccessBlock,
    pakkuLock: PakkuLock
)
{
    // Exist
    var project = this ?: return onError.error("Project not found")
    var isRecommended = true

    // Already added
    if (pakkuLock.isProjectAdded(project))
    {
        return onError.error("Could not add ${project.slug}. It is already added")
    }

    for (platform in Multiplatform.platforms)
    {
        // Check if project is on each platform
        if (project.isNotOnPlatform(platform))
        {
            onError.error("${project.slug} was not found on ${platform.name}")

            // Retry
            val project2 =  onRetry.retryWith(platform)
            if (project2 != null && project2.hasFilesOnPlatform(platform)) project += project2
            continue
        }

        // Check if project has files on each platform
        if (project.hasNoFilesOnPlatform(platform))
        {
            onError.error("No files for ${project.slug} found on ${platform.name}")
            isRecommended = false
        }
    }

    // Check if project has any files at all
    if (project.hasNoFiles())
    {
        onError.error("No files found for ${project.slug} ${pakkuLock.getMcVersions()}")
        isRecommended = false
    }

    // Check if project files match across platforms
    if (project.fileNamesNotMatchAcrossPlatforms(Multiplatform.platforms))
    {
        onError.error("${project.slug} versions do not match across platforms")
        onError.error(json.encodeToString(project.files.map { it.fileName }))
        isRecommended = false
    }

    onSuccess.success(project, isRecommended, RequestHandlers(onError, onRetry, onSuccess))
}

data class RequestHandlers(
    val onError: ErrorBlock,
    val onRetry: RetryBlock,
    val onSuccess: SuccessBlock
)

fun interface ErrorBlock
{
    fun error(error: String)
}

fun interface RetryBlock
{
    fun retryWith(platform: Platform): Project?
}

fun interface SuccessBlock
{
    fun success(project: Project, isRecommended: Boolean, ctx: RequestHandlers)
}