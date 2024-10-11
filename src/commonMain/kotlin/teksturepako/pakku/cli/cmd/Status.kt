package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.*
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.update.updateMultipleProjectsWithFiles
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
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
            terminal.pError(it)
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
            terminal.print("Managing ${configFile.getName()} modpack")

            if (configFile.getVersion().isNotBlank())
            {
                terminal.print(", version ${configFile.getVersion()}")
            }

            if (configFile.getAuthor().isNotBlank())
            {
                terminal.print(", by ${configFile.getAuthor()}")
            }

            terminal.println()
        }

        terminal.println(buildString {
            val mcVer = lockFile.getMcVersions()
            val loaders = lockFile.getLoadersWithVersions().map { (loaderName, loaderVersion) ->
                buildString {
                    append(loaderName)
                    if (loaderVersion.isNotBlank()) append("-$loaderVersion")
                }
            }

            append("on Minecraft " + (if (mcVer.size > 1) "versions" else "version") + " ${mcVer.toMsg()}, ")
            append((if (loaders.size > 1) "loaders" else "loader") + " ${loaders.toMsg()}, ")
            append("targeting " + (if (platforms.size > 1) "platforms" else "platform") + " ${platforms.toMsg()}")
            append(".")
        })

        val progressBar = progressBarLayout(spacing = 2) {
            spinner(Spinner.Dots())
        }.animateInCoroutine(terminal)

        launch { progressBar.execute() }

        val currentProjects = lockFile.getAllProjects()
        val updatedProjects =
            updateMultipleProjectsWithFiles(
                lockFile.getMcVersions(),
                lockFile.getLoaders(),
                currentProjects.toMutableSet(),
                ConfigFile.readOrNull(),
                numberOfFiles = 1
            ).getOrElse {
                terminal.pError(it)
                echo()
                return@runBlocking
            }

        progressBar.clear()

        fun projects()
        {
            terminal.println(grid {
                for (updatedProject in updatedProjects)
                {
                    row {
                        cell(" ".repeat(3) + updatedProject.getFlavoredSlug())
                        cell(updatedProject.getFlavoredName(terminal.theme))
                        cell(updatedProject.type.name)
                        updatedProject.side?.let { cell(it.name) }
                    }

                    val currentProject = currentProjects.firstOrNull { it isAlmostTheSameAs updatedProject }
                    if (currentProject == null) continue

                    var filesUpdated = false

                    for (provider in updatedProject.getProviders())
                    {
                        val cFile = currentProject.getFilesForProvider(provider).firstOrNull()?.fileName
                        val uFile = updatedProject.getFilesForProvider(provider).firstOrNull()?.fileName

                        if (cFile == null || uFile == null || cFile == uFile) continue

                        val (cDiffFile, uDiffFile) = coloredStringDiff(cFile, uFile)

                        filesUpdated = true

                        row {
                            cell(" ".repeat(6) + dim("${provider.shortName}_file:")) {
                                align = TextAlign.RIGHT
                            }
                            cell(cDiffFile) { align = TextAlign.CENTER }
                            cell(dim("->")) { align = TextAlign.CENTER }
                            cell(uDiffFile) { align = TextAlign.LEFT }
                        }
                    }

                    if (filesUpdated) row()
                }
            })
        }

        when
        {
            updatedProjects.isEmpty() && currentProjects.isNotEmpty() ->
            {
                terminal.pSuccess("All projects are up to date.")

                echo()
            }
            updatedProjects.size == 1                                 ->
            {
                terminal.pInfo("Following project has a new version available:")
                terminal.println(
                    " ".repeat(2) + "(use \"pakku update"
                        + " ${dim(updatedProjects.firstOrNull()?.slug?.values?.firstOrNull())}\""
                        + " to update the project)"
                )
                projects()
            }
            else ->
            {
                terminal.pInfo("Following projects have a new version available:")
                terminal.println(
                    " ".repeat(2) + "(use \"pakku update ${dim("[<projects>]...")}\" to update"
                        + " projects individually or \"pakku update ${dim("-a")}\" to update all projects)"
                )
                projects()
            }
        }
    }
}