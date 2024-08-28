package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.ui.*
import teksturepako.pakku.typoSuggester

class Rm : CliktCommand("Remove projects")
{
    private val projectArgs: List<String> by argument("projects").multiple()
    private val allFlag: Boolean by option("-a", "--all", help = "Remove all projects").flag()
    private val noDepsFlag: Boolean by option("-D", "--no-deps", help = "Ignore resolving dependencies").flag()

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
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
                        val slugs = lockFile.getAllProjects().flatMap { it.slug.values }

                        typoSuggester(arg, slugs).firstOrNull()?.let { realArg ->
                            if (ynPrompt("Do you mean '$realArg'?", terminal))
                            {
                                remove(lockFile.getProject(realArg), realArg)
                            }
                        }
                    }
                },
                onRemoval = { project, isRecommended ->
                    val slugMsg = project.getFlavoredSlug()

                    if (ynPrompt("Do you want to remove $slugMsg?", terminal, isRecommended))
                    {
                        lockFile.remove(project)
                        lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                        terminal.pDanger("$slugMsg removed")
                    }
                },
                onDepRemoval = { dependency, isRecommended ->
                    if (noDepsFlag) return@createRemovalRequest
                    val slugMsg = dependency.getFlavoredSlug()

                    if (isRecommended || ynPrompt("Do you want to remove $slugMsg?", terminal, false))
                    {
                        lockFile.remove(dependency)
                        lockFile.removePakkuLinkFromAllProjects(dependency.pakkuId!!)
                        terminal.pInfo("$slugMsg removed")
                    }
                },
                lockFile
            )
        }

        if (allFlag)
        {
            if (ynPrompt("Do you really want to remove all projects?", terminal))
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

        lockFile.write()?.let { terminal.pError(it) }
    }
}