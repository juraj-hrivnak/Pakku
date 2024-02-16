package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.UpdateStrategy

class Set : CliktCommand("Set various properties of your pack or projects")
{
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


    private val packNameOpt: String? by option(
        "-n", "--name",
        help = "Change the name of the pack"
    )

    private val targetOpt: String? by option(
        "-t", "--target",
        help = "Change the target of the pack"
    ).choice("curseforge", "modrinth", "multiplatform", ignoreCase = true)

    private val mcVersionsOpts: List<String>? by option(
        "-v", "--mc-versions",
        help = "Change the minecraft versions"
    ).varargValues()

    private val loadersOpts: List<String>? by option(
        "-l", "--loaders",
        help = "Change the mod loaders"
    ).varargValues()

    // TODO: Refactor this nested mess
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        /** Projects */
        if (projectArgs.isNotEmpty())
        {
            projectArgs.map { arg ->
                pakkuLock.getProject(arg)?.apply {
                    /** Side */
                    sideOpt?.let { opt ->
                        ProjectSide.valueOf(opt.uppercase())
                    }?.let {
                        side = it
                        terminal.success("'side' set to '$it' for ${this.slug}")
                    }

                    /** Update strategy */
                    updateStrategyOpt?.let { opt ->
                        UpdateStrategy.valueOf(opt.uppercase())
                    }?.let {
                        updateStrategy = it
                        terminal.success("'update_strategy' set to '$it' for ${this.slug}")
                    }

                    /** Redistribution */
                    redistributableOpt?.let { opt ->
                        redistributable = opt
                        terminal.success("'redistributable' set to '$opt' for ${this.slug}")
                    }
                }
            }
        }

        /** Pack name */
        packNameOpt?.let {
            pakkuLock.setPackName(it)
            terminal.success("'pack_name' set to '$it'")
        }

        /** Target */
        targetOpt?.let {
            pakkuLock.setPlatformTarget(it)
            terminal.success("'target' set to '$it'")
        }

        /** Minecraft versions */
        mcVersionsOpts?.let { versions ->
            var failed = false

            pakkuLock.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.mcVersions.none { it in versions })
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
                pakkuLock.setMcVersions(versions)
                terminal.success("'mc_version' set to $versions")
            }
        }

        /** Loaders */
        loadersOpts?.let { loaders ->
            var failed = false

            pakkuLock.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.loaders.none { it in loaders })
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
                pakkuLock.setModLoaders(loaders)
                terminal.success("'loaders' set to $loaders")
            }
        }

        pakkuLock.write()
        echo()
    }
}