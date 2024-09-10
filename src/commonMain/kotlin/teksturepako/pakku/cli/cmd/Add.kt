package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.fold
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.NotFoundOnPlatform
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.splitProjectArg
import teksturepako.pakku.cli.cmd.subcmd.Prj
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.*

class Add : CliktCommand()
{
    override fun help(context: Context) = "Add projects"

    private val projectArgs: List<String> by argument("projects", help = "Projects to add").multiple()
    private val noDepsFlag: Boolean by option("-D", "--no-deps", help = "Ignore resolving dependencies").flag()

    private val flags by findOrSetObject { mutableMapOf<String, Boolean>() }

    init
    {
        this.subcommands(Prj())
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

        suspend fun add(projectIn: Project?, args: Triple<String, String, String?>, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOnPlatform, args: Triple<String, String, String?>)
            {
                val prompt = promptForProject(error.platform, terminal, lockFile, args.third)
                    ?: return add(projectIn, args, strict = false)

                val (promptedProject, promptedArgs) = prompt
                if (promptedProject == null) return terminal.pError(ProjNotFound(), promptedArgs.first)

                (error.project + promptedProject).fold( // Combine projects
                    failure = { terminal.pError(it) },
                    success = { add(it, promptedArgs) }
                )
            }

            projectIn.createAdditionRequest(
                onError = { error ->
                    terminal.pError(error, arg = args.first)

                    if (error is NotFoundOnPlatform && strict)
                    {
                        handleMissingProject(error, args)
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

        for ((projectIn, args) in projectArgs.map { arg ->
            val (input, fileId) = splitProjectArg(arg)

            projectProvider.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), input, fileId
            ) to Triple(arg, input, fileId)
        })
        {
            add(projectIn, args)
            echo()
        }

        lockFile.write()?.let { terminal.pError(it) }
    }
}