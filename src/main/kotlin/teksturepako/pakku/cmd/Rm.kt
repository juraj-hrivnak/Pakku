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
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.typoSuggester

class Rm : CliktCommand("Remove mods")
{
    private val mods: List<String> by argument().multiple()
    private val all: Boolean by option("-a", "--all", help = "Remove all mods").flag()

    override fun run() = runBlocking {
        if (all) PakkuLock.handle { data ->
            if (YesNoPrompt("Do you really want to remove all mods?", terminal).ask() == true)
            {
                echo()
                terminal.danger("All mods removed")
                data.projects.clear()
                echo()
            }
        }
        else mods.map { arg ->
            async {
                Multiplatform.requestProject(arg) to arg
            }
        }.forEach {
            val (project, arg) = it.await()

            if (project != null)
            {
                if (YesNoPrompt("Do you want to remove ${project.slug}?", terminal, true).ask() == true)
                {
                    echo()
                    terminal.danger("${project.slug} removed")
                    PakkuLock.removeProject(project)
                }
            } else
            {
                terminal.warning("$arg not found")
                PakkuLock.get { data ->
                    data.projects.map { project -> project.slug.values }.flatten()
                }.also { args ->
                    typoSuggester(arg, args).first().let { arg ->
                        if (arg.isNotEmpty()) terminal.warning("Did you mean $arg?")
                    }

                }
            }
            echo()
        }
    }

}