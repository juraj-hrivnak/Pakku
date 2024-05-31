package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.table.grid
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.getFlavoredName
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.cli.ui.getFlavoredUpdateMsg

class Ls : CliktCommand("List projects")
{
    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projects = lockFile.getAllProjects()
        val platforms: List<Platform> = lockFile.getPlatforms().getOrDefault(listOf())

        val newProjects = Multiplatform.updateMultipleProjectsWithFiles(
            lockFile.getMcVersions(), lockFile.getLoaders(), projects.toMutableSet(), ConfigFile.readOrNull(), numberOfFiles = 1
        )

        terminal.println(grid {
            for (project in projects)
            {
                val deps: String = when
                {
                    project.pakkuLinks.size > 1  -> "${project.pakkuLinks.size} deps"
                    project.pakkuLinks.size == 1 -> "1 dep "
                    else                         -> "      "
                }

                row(
                    deps,
                    project.getFlavoredSlug(),
                    "${project.getFlavoredUpdateMsg(newProjects)}${project.getFlavoredName()}",
                    project.type.name,
                    project.side?.name
                )
            }
        })

        echo()
        terminal.info("Projects total: ${projects.size}")
        echo()
    }
}
