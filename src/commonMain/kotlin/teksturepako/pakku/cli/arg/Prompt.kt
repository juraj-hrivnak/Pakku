package teksturepako.pakku.cli.arg

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

suspend fun promptForProject(
    provider: IProjectProvider, terminal: Terminal, lockFile: LockFile, fileId: String? = null
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
