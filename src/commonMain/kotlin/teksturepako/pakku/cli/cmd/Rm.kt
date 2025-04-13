package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.danger
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.*
import teksturepako.pakku.typoSuggester

class Rm : CliktCommand()
{
    override fun help(context: Context) = "Remove projects"

    private val projectArgs: List<String> by argument("projects", help = "Projects to remove").multiple()
    private val allFlag: Boolean by option("-a", "--all", help = "Remove all projects").flag()
    private val noDepsFlag: Boolean by option("-D", "--no-deps", help = "Ignore resolving dependencies").flag()

    override val printHelpOnEmptyArgs = true

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        suspend fun remove(projectIn: Project?, arg: String)
        {
            projectIn.createRemovalRequest(
                onError = { error ->
                    terminal.pError(error, arg)

                    if (error is ProjNotFound)
                    {
                        val slugs = lockFile.getAllProjects().flatMap { it.slug.values + it.name.values }

                        typoSuggester(arg, slugs).firstOrNull()?.let { realArg ->
                            if (terminal.ynPrompt("Do you mean '$realArg'?"))
                            {
                                remove(lockFile.getProject(realArg), realArg)
                            }
                        }
                    }
                },
                onRemoval = { project, isRecommended ->
                    val projMsg = project.getFullMsg()

                    if (terminal.ynPrompt("Do you want to remove $projMsg?", isRecommended))
                    {
                        lockFile.remove(project)
                        lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                        terminal.pDanger("$projMsg removed")
                    }
                },
                onDepRemoval = { dependency, isRecommended ->
                    if (noDepsFlag) return@createRemovalRequest
                    val projMsg = dependency.getFullMsg()

                    val question = offset(text = "Do you want to remove $projMsg?", offset = 1)

                    if (terminal.ynPrompt(question, default = isRecommended))
                    {
                        lockFile.remove(dependency)
                        lockFile.removePakkuLinkFromAllProjects(dependency.pakkuId!!)
                        terminal.pInfo("$projMsg removed", offset = 1)
                    }
                },
                lockFile
            )
        }

        if (allFlag)
        {
            if (terminal.ynPrompt("Do you really want to remove all projects?"))
            {
                echo()
                lockFile.removeAllProjects()
                terminal.danger("All projects removed")
                echo()
            }
        }
        else for ((projectIn, arg) in projectArgs.map { arg ->
            lockFile.getProject(arg) to arg
        })
        {
            remove(projectIn, arg)
            echo()
        }

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
    }
}