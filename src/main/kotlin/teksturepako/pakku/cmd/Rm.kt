package teksturepako.pakku.cmd

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
        if (all)
        {
            if (YesNoPrompt("Do you really want to remove all projects?", terminal).ask() == true)
            {
                echo()
                PakkuLock.getAllProjects().forEach {
                    PakkuLock.removeProject(it)
                    terminal.danger("${it.slug} removed")
                }
                echo()
            }
        }
        else for (deferred in projects.map { arg ->
            async {
                PakkuLock.getProject(arg) to arg
            }
        })
        {
            val (project, arg) = deferred.await()

            if (project != null)
            {
                val linkedProjects = PakkuLock.getLinkedProjects(project.pakkuId!!, project)

                if (linkedProjects.isEmpty())
                {
                    if (YesNoPrompt("Do you want to remove ${project.slug}?", terminal, true).ask() == false) continue
                } else
                {
                    terminal.warning("$arg is required by ${linkedProjects.map { it.slug }}")
                    if (YesNoPrompt("Do you want to remove it?", terminal, false).ask() == false) continue
                }

                PakkuLock.removeProject(project)
                terminal.danger("${project.slug} removed")

                dependencies@ for (pakkuLink in project.pakkuLinks)
                {
                    val dep = PakkuLock.getProjectByPakkuId(pakkuLink) ?: continue@dependencies
                    val linkedProjects2 = PakkuLock.getLinkedProjects(dep.pakkuId!!)

                    if (linkedProjects2.isNotEmpty())
                    {
                        terminal.warning("${dep.slug} is required by ${linkedProjects2.map { it.slug }}")
                        if (YesNoPrompt("Do you want to remove it?", terminal, false).ask() == false)
                            continue@dependencies
                    }

                    PakkuLock.removeProject(dep)
                    PakkuLock.removePakkuLink(dep.pakkuId!!)
                    terminal.danger("${dep.slug} removed")
                }

                PakkuLock.removePakkuLink(project.pakkuId!!)
            } else
            {
                terminal.warning("$arg not found")
                PakkuLock.get { data ->
                    data.projects.map { project -> project.slug.values }.flatten()
                }.also { args ->
                    typoSuggester(arg, args).firstOrNull()?.let { arg ->
                        terminal.warning("Did you mean $arg?")
                    }
                }
            }
            echo()
        }
    }

}