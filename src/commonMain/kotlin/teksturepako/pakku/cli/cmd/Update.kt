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
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pSuccess

class Update : CliktCommand("Update projects")
{
    private val projectArgs: List<String> by argument("projects", help = "Projects to update").multiple()
    private val allFlag: Boolean by option("-a", "--all", help = "Update all projects").flag()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val currentProjects = if (allFlag)
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
            lockFile.getMcVersions(), lockFile.getLoaders(), currentProjects.toMutableSet(), ConfigFile.readOrNull(), numberOfFiles = 1
        )

        for (updatedProject in updatedProjects)
        {
            lockFile.update(updatedProject)
            terminal.pSuccess("${updatedProject.getFullMsg()} updated")
        }

        if (updatedProjects.isEmpty() && currentProjects.isNotEmpty())
        {
            when
            {
                allFlag || projectArgs.isEmpty() -> terminal.pSuccess("All projects are up to date")
                currentProjects.size == 1        ->
                    terminal.pSuccess("${currentProjects.first().getFullMsg()} is up to date")
                else                             ->
                    terminal.pSuccess("${currentProjects.map { it.getFullMsg() }} are up to date")
            }
        }

        echo()
        lockFile.write()
    }
}