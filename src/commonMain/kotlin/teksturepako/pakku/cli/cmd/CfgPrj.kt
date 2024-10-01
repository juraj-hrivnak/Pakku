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
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.cli.ui.pError

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

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readOrNew()

        // -- PROJECTS --

        if (projectArgs.isNotEmpty())
        {
            val projects = configFile.projects

            for (arg in projectArgs)
            {
                val project = lockFile.getProject(arg)
                if (project == null)
                {
                    terminal.pError(ActionError.ProjNotFound(), arg)
                    continue
                }

                val projectConfig = projects.getOrPut(arg) {
                    ConfigFile.ProjectConfig()
                }

                projectConfig.apply {
                    typeOpt?.let {
                        type = it
                        terminal.success("'type' set to '$it' for $arg")
                    }

                    sideOpt?.let {
                        side = it
                        terminal.success("'side' set to '$it' for $arg")
                    }

                    updateStrategyOpt?.let {
                        updateStrategy = it
                        terminal.success("'update_strategy' set to '$it' for $arg")
                    }

                    redistributableOpt?.let { opt ->
                        redistributable = opt
                        terminal.success("'redistributable' set to '$opt' for $arg")
                    }

                    subpathOpt?.let { opt ->
                        subpath = opt
                        terminal.success("'subpath' set to '$opt' for $arg")
                    }
                }
            }
        }

        configFile.write()?.let {
            terminal.pError(it)
            echo()
            return@runBlocking
        }
        echo()
    }
}