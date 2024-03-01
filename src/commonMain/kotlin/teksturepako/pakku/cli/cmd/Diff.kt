package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.projects.containsNotProject

class Diff : CliktCommand("Diff projects in modpack")
{
    private val oldPathArg by argument("path")
    private val newPathArg by argument("path")

    override fun run(): Unit = runBlocking {
        val oldPakkuLock = PakkuLock.readToResultFrom(oldPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val newPakkuLock = PakkuLock.readToResultFrom(newPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        newPakkuLock.getAllProjects()
            .mapNotNull { newProject ->
                if (oldPakkuLock.getAllProjects() containsNotProject newProject)
                {
                    newProject
                }
                else null
            }
            .mapNotNull { it.name.values.firstOrNull() }
            .forEach { terminal.success("+ $it") }

        oldPakkuLock.getAllProjects()
            .mapNotNull { oldProject ->
                if (newPakkuLock.getAllProjects() containsNotProject oldProject)
                {
                    oldProject
                }
                else null
            }
            .mapNotNull { it.name.values.firstOrNull() }
            .forEach { terminal.danger("- $it") }

        echo()
    }
}