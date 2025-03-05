package teksturepako.pakku.cli.arg

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType

suspend fun promptForProject(
    provider: Provider, terminal: Terminal, lockFile: LockFile, fileId: String? = null, projectType: ProjectType? = null
): Result<Pair<Project, ProjectArg>, ActionError>
{
    val prompt: String? = StringPrompt("Specify ${provider.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return Err(EmptyArg(provider.name))

    val arg: ProjectArg = mapProjectArg(prompt).getOrElse { return Err(it) }

    return arg.fold(
        commonArg = { commonArg ->
            val project = provider.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), commonArg.input, commonArg.fileId ?: fileId, projectType = projectType
            ).getOrElse { return@fold Err(it) }

            Ok(project to commonArg)
        },
        gitHubArg = { gitHubArg ->
            val project = GitHub.requestProjectWithFiles(
                listOf(), listOf(), "${gitHubArg.owner}/${gitHubArg.repo}", gitHubArg.tag ?: fileId, projectType = projectType
            ).getOrElse { return@fold Err(it) }

            Ok( project to gitHubArg)
        }
    )
}

var overrideYes = false

fun Terminal.ynPrompt(question: String, default: Boolean? = null): Boolean
{
    return if (overrideYes) true else YesNoPrompt(question, this, default).ask() == true
}
