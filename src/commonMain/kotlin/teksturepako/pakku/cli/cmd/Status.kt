package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.containsProject
import teksturepako.pakku.cli.ui.*


class Status: CliktCommand("Get status of your modpack")
{
    private val verboseFlag by option("-v", "--verbose", help = "Give the output in the verbose-format").flag()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            null
        }

        if (configFile != null)
        {
            terminal.pInfo("Managing '${strong(configFile.getName())}' modpack, " +
                    "version '${strong(configFile.getVersion())}', " +
                    "by '${strong(configFile.getAuthor())}'"
            )
        }

        val currentProjects = lockFile.getAllProjects()
        val updatedProjects =
            Multiplatform.updateMultipleProjectsWithFiles(
                lockFile.getMcVersions(),
                lockFile.getLoaders(),
                currentProjects.toMutableSet(),
                ConfigFile.readOrNull(),
                numberOfFiles = 1
            )

        fun projStatus()
        {
            terminal.println(grid {
                currentProjects.filter { updatedProjects containsProject it }.map { project ->
                    row(project.getFlavoredSlug(), project.getFlavoredName())

                    val updatedProject = updatedProjects.find { it isAlmostTheSameAs project }
                    updatedProject?.run {

                    }
                }
            })
        }

        when
        {
            updatedProjects.isEmpty() && currentProjects.isNotEmpty() ->
            {
                terminal.pSuccess("All projects are up to date.")
            }
            updatedProjects.size == 1                                 ->
            {
                terminal.pInfo("Following project has a new version available:")
                projStatus()
            }
            else ->
            {
                terminal.pInfo("Following projects have a new version available:")
                projStatus()
            }
        }

        echo()
    }
}