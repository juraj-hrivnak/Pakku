package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.containsProject
import teksturepako.pakku.cli.ui.*


class Status: CliktCommand()
{
    override fun help(context: Context) = "Get status of your modpack"

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

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        if (configFile != null)
        {
            val msg = buildString {
                append("Managing '${strong(configFile.getName())}' modpack")

                if (configFile.getVersion().isNotBlank())
                {
                    append("; version '${strong(configFile.getVersion())}'")
                }

                if (configFile.getAuthor().isNotBlank())
                {
                    append("; by '${strong(configFile.getAuthor())}'")
                }
            }

            terminal.pInfo(msg)
        }

        terminal.pInfo(buildString {
            val mcVer = lockFile.getMcVersions()
            val loaders = lockFile.getLoadersWithVersions()

            append("On Minecraft " + (if (mcVer.size > 1) "versions" else "version") + " ${strong(mcVer)}; ")
            append((if (loaders.size > 1) "loaders" else "loader") + " ${strong(loaders)}; ")
            append("targeting " + (if (platforms.size > 1) "platforms" else "platform") + " ${strong(platforms)}")
        })

        val currentProjects = lockFile.getAllProjects()
        val updatedProjects =
            updateMultipleProjectsWithFiles(
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
                    row(project.getFlavoredSlug(), project.getFlavoredName(terminal.theme))

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