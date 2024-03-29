package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Multiplatform

class Update : CliktCommand("Update projects")
{
    private val projectArgs: List<String> by argument("projects").multiple()
    private val allFlag: Boolean by option("-a", "--all", help = "Update all projects").flag()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val oldProjects = if (allFlag)
        {
            lockFile.getAllProjects()
        }
        else
        {
            projectArgs.mapNotNull { projectArg ->
                lockFile.getProject(projectArg).also {
                    if (it == null) terminal.danger("$projectArg not found")
                }
            }
        }

        val updatedProjects = Multiplatform.updateMultipleProjectsWithFiles(
            lockFile.getMcVersions(), lockFile.getLoaders(), oldProjects.toMutableSet(), ConfigFile.readOrNull(), numberOfFiles = 1
        )

        for (updatedProject in updatedProjects)
        {
            lockFile.update(updatedProject)
            terminal.success("${updatedProject.slug} updated")
        }

        if (updatedProjects.isEmpty() && oldProjects.isNotEmpty())
        {
            when
            {
                allFlag || projectArgs.isEmpty() -> terminal.success("All projects are up to date")
                oldProjects.size == 1            -> terminal.success("${oldProjects.first().slug} is up to date")
                else                             ->
                {
                    terminal.success("${oldProjects.map { it.slug }} are up to date")
                }
            }
        }

        echo()
        lockFile.write()
    }
}