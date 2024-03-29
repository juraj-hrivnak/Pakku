package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.ynPrompt

class Rm : CliktCommand("Remove projects")
{
    private val projectArgs: List<String> by argument("projects").multiple()
    private val allFlag: Boolean by option("-a", "--all", help = "Remove all projects").flag()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
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
        else for (pair in projectArgs.map { arg ->
            lockFile.getProject(arg) to arg
        })
        {
            val (projectIn, arg) = pair

            projectIn.createRemovalRequest(
                onWarning = { warning -> terminal.warning(warning) },
                onRemoval = { project, isRecommended ->
                    if (ynPrompt("Do you want to remove ${project.slug}?", terminal, isRecommended))
                    {
                        lockFile.remove(project)
                        lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                        terminal.danger("${project.slug} removed")
                    }
                },
                onDepRemoval = { dependency, isRecommended ->
                    if (isRecommended || ynPrompt("Do you want to remove ${dependency.slug}?", terminal, false))
                    {
                        lockFile.remove(dependency)
                        lockFile.removePakkuLinkFromAllProjects(dependency.pakkuId!!)
                        terminal.info("${dependency.slug} removed")
                    }
                },
                arg, lockFile
            )

            echo()
        }
        lockFile.write()
    }
}