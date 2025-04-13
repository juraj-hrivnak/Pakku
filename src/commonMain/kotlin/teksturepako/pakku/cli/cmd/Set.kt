package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.pError

class Set : CliktCommand()
{
    override val printHelpOnEmptyArgs = true

    override fun help(context: Context) = "Set properties of the lock file"

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
                    if (file.loaders.isNotEmpty() && file.loaders.none { it in loaders || it in Platform.validLoaders })
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

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
        echo()
    }
}