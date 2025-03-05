package teksturepako.pakku.cli.arg

import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.ui.pSuccess

suspend fun Terminal.promptForProject(
    provider: Provider, lockFile: LockFile, fileId: String? = null, projectType: ProjectType? = null
): Result<Pair<Project, ProjectArg>, ActionError>
{
    val prompt: String? = StringPrompt("Specify ${provider.name}", this).ask()

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

suspend fun Terminal.promptForCurseForgeApiKey(): ActionError?
{
    this.println()

    this.println(Markdown("""
        The API key can be generated in the [CurseForge for Studios](https://console.curseforge.com/) developer console.

        1. Login to the developer console
        2. Go to the "API keys" tab
        3. Copy your API key
    """.trimIndent()))

    this.println()

    val prompt: String? = StringPrompt("Please enter the CurseForge API key", this).ask()

    if (prompt.isNullOrBlank()) return EmptyArg("CurseForge API key")

    CredentialsFile.update(curseForgeApiKey = prompt)?.onError { return it }

    this.pSuccess("CurseForge API key successfully configured.")

    return null
}