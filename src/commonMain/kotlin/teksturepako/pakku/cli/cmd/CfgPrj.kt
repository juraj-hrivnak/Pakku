package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.terminal.danger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess

class CfgPrj : CliktCommand("prj")
{
    override fun help(context: Context) = "Configure projects"

    override fun aliases(): Map<String, List<String>> = mapOf(
        "projects" to listOf()
    )

    override val printHelpOnEmptyArgs = true

    private val projectArgs: List<String> by argument("projects")
        .help("Projects to configure")
        .multiple()

    private val typeOpt: ProjectType? by option("-t", "--type")
        .help("Change the type of a project")
        .enum<ProjectType>()

    private val sideOpt: ProjectSide? by option("-s", "--side")
        .help("Change the side of a project")
        .enum<ProjectSide>()

    private val updateStrategyOpt: UpdateStrategy? by option("-u", "--update-strategy")
        .help("Change the update strategy of a project")
        .enum<UpdateStrategy>()

    private val redistributableOpt: Boolean? by option("-r", "--redistributable")
        .help("Change whether the project can be redistributed")
        .boolean()

    private val subpathOpt: String? by option("-p", "--subpath", metavar = "path")
        .help("Change the subpath of the project")

    private val aliasOpt: String? by option("-a", "--alias", metavar = "alias")
        .help("Add alias to the project")

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readOrNew()

        projectArgs.map { arg ->
            if (lockFile.getProject(arg) != null) Ok(arg) else Err(ProjNotFound())
        }.mapNotNull { result ->
            result.onFailure { terminal.pError(it) }.get()
        }.forEach { arg ->
            val projectConfig = configFile.projects.getOrPut(arg) {
                ConfigFile.ProjectConfig()
            }

            projectConfig.apply {
                typeOpt?.let {
                    type = it
                    terminal.pSuccess("'projects.$arg.type' set to '$it'.")
                    echo()
                }

                sideOpt?.let {
                    side = it
                    terminal.pSuccess("'projects.$arg.side' set to '$it'.")
                    echo()
                }

                updateStrategyOpt?.let {
                    updateStrategy = it
                    terminal.pSuccess("'projects.$arg.update_strategy' set to '$it'.")
                    echo()
                }

                redistributableOpt?.let { opt ->
                    redistributable = opt
                    terminal.pSuccess("'projects.$arg.redistributable' set to '$opt'.")
                    echo()
                }

                subpathOpt?.let { opt ->
                    subpath = opt
                    terminal.pSuccess("'projects.$arg.subpath' set to '$opt'.")
                    echo()
                }

                aliasOpt?.let { opt ->
                    if (aliases == null) aliases = mutableSetOf()
                    aliases!!.add(opt)
                    terminal.pSuccess("'projects.$arg.aliases' add '$opt'.")
                    echo()
                }
            }
        }

        configFile.write()?.let {
            terminal.pError(it)
            echo()
            return@runBlocking
        }
    }
}