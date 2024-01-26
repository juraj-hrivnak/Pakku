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
import teksturepako.pakku.api.projects.UpdateStrategy

class Set : CliktCommand("Set various properties of the pack or projects")
{
    private val projectArgs: List<String> by argument("projects").multiple()

    private val updateStrategyOpt: String? by option(
        "-u", "--update-strategy",
        help = "Change the the update strategy of a project"
    ).choice("latest", "none")

    private val canRedistributeOpt: Boolean? by option(
        "-r", "--can-redistribute",
        help = "Change whether the project can be redistributed"
    ).boolean()


    private val packNameOpt: String? by option(
        "-n", "--name",
        help = "Change the name of the pack"
    )

    private val targetOpt: String? by option(
        "-t", "--target",
        help = "Change the target of the pack"
    ).choice("curseforge", "modrinth", "multiplatform")

    private val mcVersionsOpts: List<String>? by option(
        "-v", "--mc-versions",
        help = "Change the minecraft versions"
    ).varargValues()

    private val loadersOpts: List<String>? by option(
        "-l", "--loaders",
        help = "Change the mod loaders"
    ).varargValues()

    // TODO: Refactor the nested mess into functional implementation
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        /** Projects */
        if (projectArgs.isNotEmpty())
        {
            projectArgs.map { arg ->
                pakkuLock.getProject(arg)?.apply {
                    /** Update strategy */
                    updateStrategyOpt?.let { opt ->
                        UpdateStrategy.valueOf(opt.uppercase())
                    }?.let {
                        updateStrategy = it
                        terminal.success("'update_strategy' set to '$it' for ${this.slug}")
                    }

                    /** Redistribution */
                    canRedistributeOpt?.let { opt ->
                        canRedistribute = opt
                        terminal.success("'can_redistribute' set to '$opt' for ${this.slug}")
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