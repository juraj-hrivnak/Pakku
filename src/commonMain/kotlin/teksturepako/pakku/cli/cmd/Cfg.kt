package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess

class Cfg : CliktCommand()
{
    override fun help(context: Context) = "Configure properties of the config file"

    init
    {
        this.subcommands(CfgPrj())
    }

    override val printHelpOnEmptyArgs = true
    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = true

    // -- MODPACK --

    private val nameOpt by option("-n", "--name")
        .help("Change the name of the modpack")

    private val versionOpt by option("-v", "--version")
        .help("Change the version of the modpack")

    private val descriptionOpt by option("-d", "--description")
        .help("Change the description of the modpack")

    private val authorOpt by option("-a", "--author")
        .help("Change the author of the modpack")

    // -- PROJECT TYPE PATHS --

    private val modsPathOpt by option("--mods-path", metavar = "path")
        .help("Change the path for the `${ProjectType.MOD}` project type")

    private val resourcePacksPathOpt by option("--resource-packs-path", metavar = "path")
        .help("Change the path for the `${ProjectType.RESOURCE_PACK}` project type")

    private val dataPacksPathOpt by option("--data-packs-path", metavar = "path")
        .help("Change the path for the `${ProjectType.DATA_PACK}` project type")

    private val worldsPathOpt by option("--worlds-path", metavar = "path")
        .help("Change the path for the `${ProjectType.WORLD}` project type")

    private val shadersPathOpt by option("--shaders-path", metavar = "path")
        .help("Change the path for the `${ProjectType.SHADER}` project type")

    override fun run(): Unit = runBlocking {

        val configFile = ConfigFile.readOrNew()

        // -- MODPACK --

        nameOpt?.let { opt ->
            configFile.setName(opt)
            terminal.pSuccess("'name' set to '$opt'.")
            echo()
        }

        versionOpt?.let { opt ->
            configFile.setVersion(opt)
            terminal.pSuccess("'version' set to '$opt'.")
            echo()
        }

        descriptionOpt?.let { opt ->
            configFile.setDescription(opt)
            terminal.pSuccess("'description' set to '$opt'.")
            echo()
        }

        authorOpt?.let { opt ->
            configFile.setAuthor(opt)
            terminal.pSuccess("'author' set to '$opt'.")
            echo()
        }

        // -- PROJECT TYPE PATHS --

        modsPathOpt?.let { opt ->
            val serialName = ProjectType.MOD.serialName

            configFile.paths[serialName] = opt
            terminal.pSuccess("'paths.$serialName' set to '$opt'.")
            echo()
        }

        resourcePacksPathOpt?.let { opt ->
            val serialName = ProjectType.RESOURCE_PACK.serialName

            configFile.paths[serialName] = opt
            terminal.pSuccess("'paths.$serialName' set to '$opt'.")
            echo()
        }

        dataPacksPathOpt?.let { opt ->
            val serialName = ProjectType.DATA_PACK.serialName

            configFile.paths[serialName] = opt
            terminal.pSuccess("'paths.$serialName' set to '$opt'.")
            echo()
        }

        worldsPathOpt?.let { opt ->
            val serialName = ProjectType.WORLD.serialName

            configFile.paths[serialName] = opt
            terminal.pSuccess("'paths.$serialName' set to '$opt'.")
            echo()
        }

        shadersPathOpt?.let { opt ->
            val serialName = ProjectType.SHADER.serialName

            configFile.paths[serialName] = opt
            terminal.pSuccess("'paths.$serialName' set to '$opt'.")
            echo()
        }

        configFile.write()?.let {
            terminal.pError(it)
            echo()
            return@runBlocking
        }
    }
}