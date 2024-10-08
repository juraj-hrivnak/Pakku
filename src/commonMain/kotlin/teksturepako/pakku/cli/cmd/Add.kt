package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.danger
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.NotFoundOn
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.*
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pError
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

    private val flags by findOrSetObject { mutableMapOf<String, Boolean>() }

    init
    {
        this.subcommands(AddPrj())
    }

    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = true

    override fun run(): Unit = runBlocking {
        // Pass flags to the context
        flags["noDepsFlag"] = noDepsFlag

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

        suspend fun add(projectIn: Project?, arg: ProjectArg, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOn, arg: ProjectArg)
            {
                val prompt = promptForProject(error.provider, terminal, lockFile, arg.fold({it.fileId}, {it.tag})).onFailure {
                    if (it is EmptyArg) return add(projectIn, arg, strict = false)
                }.getOrElse {
                    return terminal.pError(it)
                }

                val (promptedProject, promptedArg) = prompt

                if (promptedProject == null) return terminal.pError(ProjNotFound(), promptedArg.rawArg)

                (error.project + promptedProject).fold( // Combine projects
                    failure = { terminal.pError(it) },
                    success = { add(it, promptedArg) }
                )
            }

            projectIn.createAdditionRequest(
                onError = { error ->
                    terminal.pError(error, arg = arg.rawArg)

                    if (error is NotFoundOn && strict)
                    {
                        handleMissingProject(error, arg)
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
                },
                lockFile, platforms, strict
            )
        }

        for ((projectIn, arg) in projectArgs.map { arg ->
            arg.fold(
                commonArg = {
                    projectProvider.requestProjectWithFiles(
                        lockFile.getMcVersions(), lockFile.getLoaders(), it.input, it.fileId
                    ) to it
                },
                gitHubArg = {
                    GitHub.requestProjectWithFiles(
                        listOf(), listOf(), "${it.owner}/${it.repo}", it.tag
                    ) to it
                }
            )
        })
        {
            add(projectIn, arg)
            echo()
        }

        lockFile.write()?.let { terminal.pError(it) }
    }
}