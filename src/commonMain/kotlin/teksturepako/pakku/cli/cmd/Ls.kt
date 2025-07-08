package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.terminal.info
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.errors.VersionsDoNotMatch
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.*

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
            terminal.pError(it)
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
            addPaddingWidthToFixedWidth = false

            column(1) {
                width = ColumnWidth.Auto
            }
            column(2) {
                width = ColumnWidth.Expand(0.5f)
            }

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

                    if (newProjects != null)
                    {
                        cell(horizontalLayout {
                            spacing = 0

                            runBlocking {
                                cell(project.getFlavoredUpdateMsg(terminal.theme, newProjects.await()))
                            }
                            cell(project.getFlavoredName(terminal.theme))
                        })
                    }
                    else
                    {
                        cell(project.getFlavoredName(terminal.theme))
                    }

                    cell(project.type.name)
                    project.side?.let { cell(it.name) }
                }
                if (project.versionsDoNotMatchAcrossProviders(project.getProviders()))
                {
                    row {
                        cell("")
                        cell(terminal.processShortErrorMsg(VersionsDoNotMatch(project)))
                    }
                    row()
                }
            }
        })

        echo()
        terminal.info("Projects total: ${projects.size}")
        echo()
    }
}
