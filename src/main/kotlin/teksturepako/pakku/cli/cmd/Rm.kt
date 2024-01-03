package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.typoSuggester

class Rm : CliktCommand("Remove projects")
{
    private val projects: List<String> by argument().multiple()
    private val all: Boolean by option("-a", "--all", help = "Remove all mods").flag()

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        if (all)
        {
            if (YesNoPrompt("Do you really want to remove all projects?", terminal).ask() == true)
            {
                echo()
                pakkuLock.removeAll()
                terminal.danger("All projects removed")
                echo()
            }
        }
        else for (deferred in projects.map { arg ->
            async {
                pakkuLock.getProject(arg) to arg
            }
        })
        {
            val (project, arg) = deferred.await()

            if (project != null)
            {
                val linkedProjects = pakkuLock.getLinkedProjects(project.pakkuId!!, project)

                if (linkedProjects.isEmpty())
                {
                    if (YesNoPrompt("Do you want to remove ${project.slug}?", terminal, true).ask() == false) continue
                } else
                {
                    terminal.warning("$arg is required by ${linkedProjects.map { it.slug }}")
                    if (YesNoPrompt("Do you want to remove it?", terminal, false).ask() == false) continue
                }

                pakkuLock.remove(project)
                terminal.danger("${project.slug} removed")

                dependencies@ for (pakkuLink in project.pakkuLinks)
                {
                    val dep = pakkuLock.getProjectByPakkuId(pakkuLink) ?: continue@dependencies
                    val linkedProjects2 = pakkuLock.getLinkedProjects(dep.pakkuId!!)

                    if (linkedProjects2.isNotEmpty())
                    {
                        terminal.warning("${dep.slug} is required by ${linkedProjects2.map { it.slug }}")
                        if (YesNoPrompt("Do you want to remove it?", terminal, false).ask() == false)
                            continue@dependencies
                    }

                    pakkuLock.remove(dep)
                    pakkuLock.removePakkuLink(dep.pakkuId!!)
                    terminal.danger("${dep.slug} removed")
                }

                pakkuLock.removePakkuLink(project.pakkuId!!)
            } else
            {
                terminal.warning("$arg not found")
                pakkuLock.getAllProjects().flatMap { it.slug.values }.also { args ->
                    typoSuggester(arg, args).firstOrNull()?.let { arg ->
                        terminal.warning("Did you mean $arg?")
                    }
                }
            }
            echo()
        }

        pakkuLock.write()
    }
}