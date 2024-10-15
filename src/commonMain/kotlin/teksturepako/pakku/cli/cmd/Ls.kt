package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.getFlavoredName
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.cli.ui.getFlavoredUpdateMsg

class Ls : CliktCommand()
{
    override fun help(context: Context) = "List projects"

    private val checkUpdatesFlag by option("-c", "--check-updates", help = "Add update info for projects").flag()
    private val nameMaxLengthOpt by option(
        "--name-max-length",
        help = "Set max length for project names"
    ).int().default(20)

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projects = lockFile.getAllProjects()

        val newProjects = if (checkUpdatesFlag) async {
            updateMultipleProjectsWithFiles(
                lockFile.getMcVersions(),
                lockFile.getLoaders(),
                projects.toMutableSet(), ConfigFile.readOrNull(), numberOfFiles = 1
            )
        } else null

        terminal.println(grid {
            for (project in projects)
            {
                val deps: String = when
                {
                    project.pakkuLinks.size > 1  -> "${project.pakkuLinks.size} deps"
                    project.pakkuLinks.size == 1 -> "1 dep  "
                    else                         -> "       "
                }

                row {
                    cell(deps)
                    cell(project.getFlavoredSlug())

                    val name = if (newProjects != null) runBlocking {
                        project.getFlavoredUpdateMsg(terminal.theme, newProjects.await()) +
                                project.getFlavoredName(terminal.theme, nameMaxLengthOpt)
                    }
                    else " " + project.getFlavoredName(terminal.theme, nameMaxLengthOpt)

                    cell(name)

                    cell(project.type.name)
                    project.side?.let { cell(it.name) }
                }
            }
        })

        echo()
        terminal.info("Projects total: ${projects.size}")
        echo()
    }
}
