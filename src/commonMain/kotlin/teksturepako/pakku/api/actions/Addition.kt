package teksturepako.pakku.api.actions

import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pSuccess

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
    val existingProject = lockFile.getProject(project)
    if (existingProject != null)
    {
        project.files.removeAll(existingProject.files)
        if (project.hasNoFiles()) return onError(AlreadyAdded(project))
    }

    // We do not have to check platform for GitHub only project
    if (project.slug.keys.size > 1 || project.slug.keys.firstOrNull() != GitHub.serialName)
    {
        for (platform in platforms)
        {
            // Check if project is on each platform
            if (project.isNotOnPlatform(platform))
            {
                if (!strict) continue
                else
                {
                    onError(NotFoundOn(project, platform))
                    return
                }
            }

            // Check if project has files on each platform
            if (project.hasNoFilesOnPlatform(platform))
            {
                onError(NoFilesOn(project, platform))
                isRecommended = false
            }
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

suspend fun Project.promptForAddition(
    lockFile: LockFile,
    terminal: Terminal,
    isRecommended: Boolean,
    noDepsFlag: Boolean,
    reqHandlers: RequestHandlers,
    projectProvider: Provider,
    platforms: List<Platform>
)
{
    val projMsg = this.getFullMsg()
    val oldProject = lockFile.getProject(this)
    val replacing = oldProject != null
    if (ynPrompt("Do you want to ${if (replacing) "replace" else "add"} $projMsg?", terminal, isRecommended))
    {
        if (replacing) lockFile.update(this) else lockFile.add(this)

        lockFile.linkProjectToDependents(this)

        if (!noDepsFlag)
        {
            this.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
        }

        terminal.pSuccess("$projMsg ${if (replacing) "replaced" else "added"}")
    }
}