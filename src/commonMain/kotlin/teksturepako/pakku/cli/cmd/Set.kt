package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.UpdateStrategy

class Set : CliktCommand()
{
    override fun help(context: Context) = "Set various properties of your modpack or projects"

    // -- PROJECTS --

    private val projectArgs: List<String> by argument("projects").multiple()

    private val sideOpt: String? by option(
        "-s", "--side",
        help = "Change the side of a project"
    ).choice("client", "server", "both", ignoreCase = true)

    private val updateStrategyOpt: String? by option(
        "-u", "--update-strategy",
        help = "Change the update strategy of a project"
    ).choice("latest", "none", ignoreCase = true)

    private val redistributableOpt: Boolean? by option(
        "-r", "--redistributable",
        help = "Change whether the project can be redistributed"
    ).boolean()

    // -- PACK --

    private val targetOpt: String? by option(
        "-t", "--target",
        help = "Change the target of the pack"
    ).choice("curseforge", "modrinth", "multiplatform", ignoreCase = true)

    private val mcVersionsOpts: List<String>? by option(
        "-v", "--mc-versions",
        help = "Change the minecraft versions"
    ).varargValues()

    private val loadersOpts: Map<String, String>? by option(
        "-l", "--loaders",
        help = "Change the mod loaders",
        metavar = "<name>=<version>"
    ).associate()

    override fun run() = runBlocking {
        val lockFile = LockFile.readOrNew()

        // -- PROJECTS --

        if (projectArgs.isNotEmpty())
        {
            for (arg in projectArgs)
            {
                val project = lockFile.getProject(arg) ?: continue

                project.apply {
                    sideOpt?.let { opt ->
                        ProjectSide.valueOf(opt.uppercase())
                    }?.let {
                        side = it
                        terminal.success("'side' set to '$it' for ${this.slug}")
                    }

                    updateStrategyOpt?.let { opt ->
                        UpdateStrategy.valueOf(opt.uppercase())
                    }?.let {
                        updateStrategy = it
                        terminal.success("'update_strategy' set to '$it' for ${this.slug}")
                    }

                    redistributableOpt?.let { opt ->
                        redistributable = opt
                        terminal.success("'redistributable' set to '$opt' for ${this.slug}")
                    }
                }
            }
        }

        // -- PACK --

        /* Target */
        targetOpt?.let {
            lockFile.setTarget(it)
            terminal.success("'target' set to '$it'")
        }

        /* Minecraft versions */
        mcVersionsOpts?.let { versions ->
            var failed = false

            lockFile.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.mcVersions.isNotEmpty() && file.mcVersions.none { it in versions })
                    {
                        terminal.danger(
                            "Can not set to $versions,"
                            + " because ${project.name.values.first()} (${file.type}) requires ${file.mcVersions}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                lockFile.setMcVersions(versions)
                terminal.success("'mc_version' set to $versions")
            }
        }

        /* Loaders */
        loadersOpts?.let { loaders ->
            if (loadersOpts.isNullOrEmpty()) return@let

            var failed = false

            lockFile.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.loaders.isNotEmpty() && file.loaders.none { it in loaders })
                    {
                        terminal.danger(
                            "Can not set to $loaders,"
                            + " because ${project.name.values.first()} (${file.type}) requires ${file.loaders}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                lockFile.setLoaders(loaders)
                terminal.success("'loaders' set to $loaders")
            }
        }

        lockFile.write()
        echo()
    }
}