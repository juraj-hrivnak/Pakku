package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.data.LockFile

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
            if (YesNoPrompt("Do you really want to remove all projects?", terminal).ask() == true)
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
                    if (YesNoPrompt("Do you want to remove ${project.slug}?", terminal, isRecommended).ask() == true)
                    {
                        lockFile.remove(project)
                        lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                        terminal.danger("${project.slug} removed")
                    }
                },
                onDepRemoval = { dependency, isRecommended ->
                    if (isRecommended || YesNoPrompt("Do you want to remove ${dependency.slug}?", terminal, false).ask() == true)
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