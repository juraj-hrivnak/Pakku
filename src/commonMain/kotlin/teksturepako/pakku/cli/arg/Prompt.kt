package teksturepako.pakku.cli.arg

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.RequestHandlers
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pSuccess

suspend fun promptForProject(
    provider: Provider, terminal: Terminal, lockFile: LockFile, fileId: String? = null
): Result<Pair<Project?, ProjectArg>, ActionError>
{
    val prompt: String? = StringPrompt("Specify ${provider.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return Err(EmptyArg(provider.name))

    val arg: ProjectArg = mapProjectArg(prompt).getOrElse { return Err(it) }

    return arg.fold(
        commonArg = {
            Ok(provider.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId ?: fileId
            ) to it)
        },
        gitHubArg = {
            Ok(GitHub.requestProjectWithFiles(
                listOf(), listOf(), "${it.owner}/${it.repo}", it.tag ?: fileId
            ) to it)
        }
    )
}

var overrideYes = false

fun ynPrompt(prompt: String, terminal: Terminal, default: Boolean? = null): Boolean
{
    return if (overrideYes) true else YesNoPrompt(prompt, terminal, default).ask() == true
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