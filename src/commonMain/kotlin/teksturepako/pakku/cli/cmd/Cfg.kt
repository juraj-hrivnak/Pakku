package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.UpdateStrategy

class Cfg : CliktCommand()
{
    override fun help(context: Context) = "Configure various options of your modpack or projects"

    // -- PROJECTS --

    private val projectArgs: List<String> by argument("projects").multiple()

    private val sideOpt: String? by option("-s", "--side")
        .help("Change the side of a project")
        .choice("client", "server", "both", ignoreCase = true)

    private val updateStrategyOpt: String? by option("-u", "--update-strategy")
        .help("Change the update strategy of a project")
        .choice("latest", "none", ignoreCase = true)

    private val redistributableOpt: Boolean? by option("-r", "--redistributable")
        .help("Change whether the project can be redistributed")
        .boolean()

    private val subpathOpt: String? by option("-p", "--subpath")
        .help("Change the subpath of the project")

    override fun run() = runBlocking {
        val lockFile = LockFile.readOrNew()
        val config = ConfigFile.readOrNew()

        // -- PROJECTS --

        if (projectArgs.isNotEmpty())
        {
            val projects = config.getProjects()

            for (arg in projectArgs)
            {
                val project = lockFile.getProject(arg)
                if (project == null)
                {
                    terminal.warning("Can't find project '$arg'")
                    continue
                }
                val projectConfig = projects.getOrPut(arg) {
                    ConfigFile.ProjectConfig(null, null, null, null, null)
                }

                projectConfig.apply {
                    sideOpt?.let { opt ->
                        ProjectSide.valueOf(opt.uppercase())
                    }?.let {
                        side = it
                        terminal.success("'side' set to '$it' for $arg")
                    }

                    updateStrategyOpt?.let { opt ->
                        UpdateStrategy.valueOf(opt.uppercase())
                    }?.let {
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

        lockFile.write()
        config.write()
        echo()
    }
}