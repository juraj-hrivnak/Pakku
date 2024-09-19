package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.EmptyArg
import teksturepako.pakku.cli.arg.ProjectArg
import teksturepako.pakku.cli.arg.mapProjectArg

suspend fun promptForProject(
    provider: IProjectProvider, terminal: Terminal, lockFile: LockFile, fileId: String? = null
): Result<Pair<Project?, ProjectArg>, ActionError>
{
    val prompt: String? = StringPrompt("Specify ${provider.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return Err(EmptyArg(provider.name))

    val arg: ProjectArg = mapProjectArg(prompt).getOrElse { return Err(it) }

    return arg.fold(
        arg = {
            Ok(provider.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId ?: fileId
            ) to it)
        },
        gitHubArg = {
            Ok(GitHub.requestProjectWithFiles(
                listOf(), listOf(), "${it.owner}/${it.repo}", fileId
            ) to it)
        }
    )
}

var overrideYes = false

fun ynPrompt(prompt: String, terminal: Terminal, default: Boolean? = null): Boolean
{
    return if (overrideYes) true else YesNoPrompt(prompt, terminal, default).ask() == true
}
