package teksturepako.pakku.cli.cmd

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

class Update : CliktCommand("Update projects")
{
    private val projects: List<String> by argument().multiple()
    private val all: Boolean by option("-a", "--all", help = "Update all projects").flag()

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        val projects = mutableListOf<Project>()

        if (all)
        {
            pakkuLock.getAllProjects().forEach { project ->
                projects.add(project)
            }
        } else
        {
            this@Update.projects.forEach { project ->
                pakkuLock.getProject(project)?.let {
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

                    platforms@ for (platform in Multiplatform.platforms)
                    {
                        project.id[platform.serialName]?.let { id ->
                            platform.requestProjectWithFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), id)?.let { updatedProject += it }
                        } ?: continue@platforms
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
                        pakkuLock.update(updatedProject)
                        terminal.success("${updatedProject.slug} updated")
                    }
                }
            } else
            {
                if (all || this@Update.projects.isEmpty()) terminal.success("All projects are up to date")
                else terminal.success("${this@Update.projects} up to date")
            }
        }

        pakkuLock.write()
        echo()
    }
}