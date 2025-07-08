package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents.Formats.RFC_1123
import teksturepako.pakku.api.actions.errors.VersionsDoNotMatch
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.*
import teksturepako.pakku.debug
import teksturepako.pakku.typoSuggester

class Insp : CliktCommand()
{
    override fun help(context: Context) = "Inspect projects"

    private val projectArgs: List<String> by argument("projects", help = "Projects to inspect").multiple()

    override val printHelpOnEmptyArgs = true

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val configFile = if (ConfigFile.exists())
        {
            ConfigFile.readToResult().getOrElse {
                terminal.pError(it)
                echo()
                return@runBlocking
            }
        }
        else null


        val cliConfig = runBlocking { CliConfig.readToResult() }
            .onFailure { error -> debug { println(error.rawMessage) } }
            .get()

        val collection = projectArgs.map { arg ->
            lockFile.getProject(arg) to arg
        }

        for ((i, j) in collection.withIndex())
        {
            val (project, arg) = j

            if (collection.size <= i + 1 && i > 0)
            {
                terminal.println("-".repeat(terminal.size.width))
                terminal.println()
            }

            terminal.insp(project, arg, lockFile, configFile, cliConfig)
        }
    }
}

private tailrec fun Terminal.insp(project: Project?, arg: String, lockFile: LockFile, configFile: ConfigFile?, cliConfig: CliConfig?)
{
    val slugs = lockFile.getAllProjects().flatMap { it.slug.values + it.name.values }

    if (project == null)
    {
        this.pError(ProjNotFound(arg))

        typoSuggester(arg, slugs).firstOrNull()?.let { realArg ->
            if (this.ynPrompt("Do you mean '$realArg'?", default = true))
            {
                this.println()
                return insp(lockFile.getProject(realArg), realArg, lockFile, configFile, cliConfig)
            }
        }

        this.println()
        return
    }

    val panelContent = grid {
        addPaddingWidthToFixedWidth = false

        column(0) {
            width = ColumnWidth(1)
        }

        row {
            cell(project.getFlavoredName(this@insp.theme)?.let { Text(it) })
        }
        row {
            cell(
                content = project.getFlavoredSlug(),
                init = { align = TextAlign.LEFT }
            )
            cell(
                content = dim(project.type),
                init = { align = TextAlign.RIGHT }
            )
            project.side?.let {
                cell(
                    content = dim(it),
                    init = { align = TextAlign.RIGHT }
                )
            }
        }
    }

    val latestFile = project.getLatestFile(project.getProviders())

    val projectFiles = project.files
        .takeIf { it.isNotEmpty() }
        ?.mapNotNull { projectFile ->
            val provider = Provider.getProvider(projectFile.type)?.shortName
                ?.createHyperlink(projectFile.getSiteUrl(lockFile))
                ?: return@mapNotNull null
            val path = projectFile.getRelativePathString(project, configFile)

            val content = grid {
                addPaddingWidthToFixedWidth = false

                row {
                    cell(
                        horizontalLayout {
                            cell(
                                content = dim("$provider=") + path,
                                init = {
                                    align = TextAlign.LEFT
                                    overflowWrap = OverflowWrap.BREAK_WORD
                                    whitespace = Whitespace.NORMAL
                                }
                            )
                            cell(
                                content = dim(projectFile.datePublished.format(RFC_1123)),
                                init = {
                                    align = TextAlign.RIGHT
                                    overflowWrap = OverflowWrap.BREAK_WORD
                                    whitespace = Whitespace.NORMAL
                                }
                            )
                        }
                    )
                }
                row()
                row {
                    cell(
                        content = projectFile.hashes?.map { (algName, hash) ->
                            dim("$algName=$hash")
                        }?.joinToString(" "),
                        init = {
                            align = TextAlign.RIGHT
                            overflowWrap = OverflowWrap.BREAK_WORD
                            whitespace = Whitespace.NORMAL
                        }
                    )
                }
            }

            val isLatestFile = latestFile == projectFile

            Panel(
                content = content,
                borderType = if (this@insp.theme == CliThemes.Ascii) BorderType.ASCII else BorderType.ROUNDED,
                borderStyle = TextStyle(color = if (isLatestFile) TextColors.green else TextColors.white),
                title = if (isLatestFile) Text(TextColors.green("current")) else null,
                titleAlign = TextAlign.LEFT,
            )
        }

    val properties = listOfNotNull(
        dim("type=") + project.type,
        project.side?.let { dim("side=") + it },
        dim("update_strategy=") + project.updateStrategy,
        dim("redistributable=") + project.redistributable,
        project.getSubpath()?.get()?.let { dim("subpath=") + it },
        project.aliases?.toMsg()?.let { dim("aliases=") + it },
    )

    this.println(grid {
        overflowWrap = OverflowWrap.BREAK_WORD
        whitespace = Whitespace.NORMAL

        row {
            cell(Text("Project"))
            cell(Panel(
                content = panelContent,
                borderType = if (this@insp.theme == CliThemes.Ascii) BorderType.ASCII else BorderType.ROUNDED,
            ))
        }
        row()
        row {
            cell(Text("Project Files", overflowWrap = OverflowWrap.BREAK_WORD, whitespace = Whitespace.NORMAL))
            cell(verticalLayout {
                projectFiles?.asIterable()?.let { cellsFrom(it) }
                if (project.versionsDoNotMatchAcrossProviders(project.getProviders()))
                {
                    cell("")
                    cell(this@insp.processShortErrorMsg(VersionsDoNotMatch(project)))
                }
            })
        }
        row()
        row {
            cell(Text("Properties"))
            cell(verticalLayout {
                cellsFrom(properties.asIterable())
            })
        }
    })

    this.println()
}