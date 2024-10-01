package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.NotFoundOn
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.*
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import kotlin.collections.fold
import kotlin.system.exitProcess
import com.github.michaelbull.result.fold as resultFold

class AddPrj : CliktCommand("prj")
{
    override fun help(context: Context) = "Specify the project precisely"

    override fun aliases(): Map<String, List<String>> = mapOf(
        "project" to listOf()
    )

    private val cfOpt: ProjectArg.CommonArg? by option(
        "--cf", "--curseforge", help = "CurseForge project slug or ID"
    ).convert {
        splitCommonArg(it).getOrElse { err ->
            terminal.pError(err)
            exitProcess(1)
        }
    }

    private val mrOpt: ProjectArg.CommonArg? by option(
        "--mr", "--modrinth", help = "Modrinth project slug or ID"
    ).convert {
        splitCommonArg(it).getOrElse { err ->
            terminal.pError(err)
            exitProcess(1)
        }
    }

    private val ghOpt: ProjectArg.GitHubArg? by option(
        "--gh", "--github", help = "GitHub repository URL or `{owner}/{repo}`"
    ).convert {
        splitGitHubArg(it).getOrElse { err ->
            terminal.pError(err)
            exitProcess(1)
        }
    }

    private val flags by requireObject<Map<String, Boolean>>()

    override fun run(): Unit = runBlocking {
        // Get flags from the parent command
        val noDepsFlag = flags.getOrElse("noDepsFlag") { false }

        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectProvider = lockFile.getProjectProvider().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        suspend fun add(projectIn: Project?, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOn)
            {
                val prompt = promptForProject(error.provider, terminal, lockFile).onFailure {
                    if (it is EmptyArg) return add(projectIn, strict = false)
                }.getOrElse {
                    return terminal.pError(it)
                }

                val (promptedProject, promptedArg) = prompt

                if (promptedProject == null) return terminal.pError(ProjNotFound(), promptedArg.rawArg)

                (error.project + promptedProject).resultFold( // Combine projects
                    failure = { terminal.pError(it) },
                    success = { add(it) }
                )
            }

            projectIn.createAdditionRequest(
                onError = { error ->
                    terminal.pError(error)

                    if (error is NotFoundOn && strict)
                    {
                        handleMissingProject(error)
                    }
                },
                onSuccess = { project, isRecommended, reqHandlers ->
                    val projMsg = project.getFullMsg()

                    if (ynPrompt("Do you want to add $projMsg?", terminal, isRecommended))
                    {
                        lockFile.add(project)
                        lockFile.linkProjectToDependents(project)

                        if (!noDepsFlag)
                        {
                            project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        }

                        terminal.pSuccess("$projMsg added")
                    }
                }, lockFile, platforms, strict
            )
        }

        val cf = cfOpt?.let {
            CurseForge.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId
            )
        }

        val mr = mrOpt?.let {
            Modrinth.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId
            )
        }

        val gh = ghOpt?.let {
            GitHub.requestProjectWithFiles(
                listOf(), listOf(), "${it.owner}/${it.repo}", it.tag
            )
        }

        val projects: List<Project?> = listOf(cf, mr, gh)

        val combinedProject: Project? = projects.fold(null as Project?) { acc, project ->
            when
            {
                acc == null     -> project
                project == null -> acc
                else            -> (acc + project).get()
            }
        }

        add(combinedProject)
        echo()

        lockFile.write()?.let { terminal.pError(it) }
    }
}