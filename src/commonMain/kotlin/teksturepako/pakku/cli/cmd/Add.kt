package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.arg.*
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pErrorOrPrompt
import teksturepako.pakku.cli.ui.pSuccess

class Add : CliktCommand()
{
    override fun help(context: Context) = "Add projects"

    private val projectArgs: List<ProjectArg> by argument("projects", help = "Projects to add").multiple().transformAll {
        it.mapNotNull x@ { input ->
            mapProjectArg(input).getOrElse { err ->
                terminal.pError(err)
                null
            }
        }
    }

    private val noDepsFlag: Boolean by option("-D", "--no-deps", help = "Ignore resolving dependencies").flag()

    private val projectTypeOpt: ProjectType? by option(
        "-t",
        "--type",
        help = "Project type of projects to add",
        metavar = "project type"
    ).enum<ProjectType>()

    private val flags by findOrSetObject { mutableMapOf<String, Boolean>() }

    init
    {
        this.subcommands(AddPrj())
    }

    override val printHelpOnEmptyArgs = true
    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = true

    override fun run(): Unit = runBlocking {
        // Pass flags to the context
        flags["noDepsFlag"] = noDepsFlag

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

        suspend fun add(projectIn: Project?, arg: ProjectArg, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOn, arg: ProjectArg)
            {
                val (promptedProject, promptedArg) = terminal.promptForProject(
                    error.provider, lockFile, arg.fold({it.fileId}, {it.tag}), projectType = projectTypeOpt
                ).onFailure {
                    if (it is EmptyArg) return add(projectIn, arg, strict = false)
                }.getOrElse {
                    return terminal.pErrorOrPrompt(it)
                }

                (error.project + promptedProject).fold( // Combine projects
                    failure = { terminal.pError(it) },
                    success = { add(it, promptedArg) }
                )
            }

            projectIn.createAdditionRequest(
                onError = { error ->
                    terminal.pError(error)

                    if (error is CurseForge.Unauthenticated)
                    {
                        terminal.promptForCurseForgeApiKey()?.onError { terminal.pError(it) }
                    }

                    if (error is NotFoundOn && strict)
                    {
                        handleMissingProject(error, arg)
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
                },
                lockFile, platforms, strict
            )
        }

        for ((projectIn, arg) in projectArgs.map { arg ->
            arg.fold(
                commonArg = {
                    projectProvider.requestProjectWithFiles(
                        lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId, projectType = projectTypeOpt
                    ) to it
                },
                gitHubArg = {
                    GitHub.requestProjectWithFiles(
                        listOf(), listOf(), "${it.owner}/${it.repo}", it.tag, projectType = projectTypeOpt
                    ) to it
                }
            )
        })
        {
            projectIn.fold(
                success = {
                    add(projectIn.get(), arg)
                    echo()
                },
                failure = { error ->
                    terminal.pErrorOrPrompt(error)
                }
            )
        }

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
    }
}