package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.michaelbull.result.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.http.RequestError
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
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
        "--gh", "--github", help = "GitHub repository URL or `<owner>/<repo>`"
    ).convert {
        splitGitHubArg(it).getOrElse { err ->
            terminal.pError(err)
            exitProcess(1)
        }
    }

    private val projectTypeOpt: ProjectType? by option(
        "-t",
        "--type",
        help = "Project type of project to add",
        metavar = "project type"
    ).enum<ProjectType>()

    override val printHelpOnEmptyArgs = true

    private val flags by requireObject<Map<String, Boolean>>()

    override fun run(): Unit = runBlocking {
        // Get flags from the parent command
        val noDepsFlag = flags.getOrElse("noDepsFlag") { false }

        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val projectProvider = lockFile.getProjectProvider().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        suspend fun add(projectIn: Project?, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOn)
            {
                val (promptedProject, promptedArg) = promptForProject(
                    error.provider, terminal, lockFile, projectType = projectTypeOpt
                ).onFailure {
                    if (it is EmptyArg) return add(projectIn, strict = false)
                }.getOrElse {
                    return terminal.pError(it)
                }

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
                onSuccess = { project, isRecommended, replacing, reqHandlers ->
                    val projMsg = project.getFullMsg()
                    val promptMessage = if (replacing == null)
                    {
                        "Do you want to add $projMsg?" to "$projMsg added"
                    }
                    else
                    {
                        val replacingMsg = replacing.getFullMsg()
                        "Do you want to replace $replacingMsg with $projMsg?" to
                                "$replacingMsg replaced with $projMsg"
                    }

                    if (terminal.ynPrompt(promptMessage.first, isRecommended))
                    {
                        if (replacing == null) lockFile.add(project) else lockFile.update(project)
                        lockFile.linkProjectToDependents(project)

                        if (!noDepsFlag)
                        {
                            project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        }

                        terminal.pSuccess(promptMessage.second)
                    }
                }, lockFile, platforms, strict
            )
        }

        val cf = cfOpt?.let {
            CurseForge.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId, projectType = projectTypeOpt
            )
        }

        val mr = mrOpt?.let {
            Modrinth.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId, projectType = projectTypeOpt
            )
        }

        val gh = ghOpt?.let {
            GitHub.requestProjectWithFiles(
                listOf(), listOf(), "${it.owner}/${it.repo}", it.tag, projectType = projectTypeOpt
            )
        }

        val projects = listOf(cf, mr, gh)

        val combinedProject: Result<Project, ActionError> = projects.fold(
            Err(ProjNotFound()) as Result<Project, ActionError>
        ) { acc, currentProject ->
            acc.onFailure { error ->
                if (error !is ProjNotFound && !(error is RequestError && error.response.status == HttpStatusCode.NotFound))
                {
                    return@fold Err(error)
                }
            }

            currentProject?.flatMap { project ->
                acc.map { accProject ->
                    accProject + project
                }.getOrElse {
                    Ok(project)
                }
            } ?: acc
        }

        if (combinedProject.isErr)
        {
            terminal.pError(combinedProject.error)
            echo()

            return@runBlocking
        }

        add(combinedProject.get())
        echo()

        lockFile.write()?.let { terminal.pError(it) }
    }
}