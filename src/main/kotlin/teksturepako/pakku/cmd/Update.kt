package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.Project

class Update : CliktCommand("Update mod/s")
{
    private val mods: List<String> by argument().multiple()
    private val all: Boolean by option("-a", "--all", help = "Update all mods").flag()

    override fun run() = runBlocking {
        val projects = mutableListOf<Project>()

        if (all)
        {
            PakkuLock.getAllProjects().forEach { project ->
                projects.add(project)
            }
        } else
        {
            mods.forEach { project ->
                PakkuLock.getProject(project)?.let {
                    projects.add(it)
                }
            }
        }

        val updatedProjects = mutableListOf<Project>()

        runBlocking {
            for (project in projects)
            {
                launch {
                    var updatedProject = project.copy(files = mutableSetOf())

                    Multiplatform.platforms.forEach { platform ->
                        platform.requestProjectWithFiles(
                            PakkuLock.getMcVersions(), PakkuLock.getLoaders(), project.id[platform.serialName]!!
                        )?.let { updatedProject += it } ?: terminal.danger("${updatedProject.slug} not found")
                    }

                    if (project != updatedProject)
                    {
                        updatedProjects.add(updatedProject)
                    }
                }
            }
        }

        runBlocking {
            if (updatedProjects.isNotEmpty())
            {
                for (updatedProject in updatedProjects)
                {
                    launch {
                        PakkuLock.updateProject(updatedProject)
                        terminal.success("${updatedProject.slug} updated")
                    }
                }
            } else
            {
                if (all) terminal.success("All mods up to date")
                else terminal.success("$mods up to date")
            }
        }

        echo()
    }
}