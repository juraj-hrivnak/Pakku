package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.terminal.prompt
import com.github.ajalt.mordant.terminal.success
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.cli.arg.promptForCurseForgeApiKey
import teksturepako.pakku.cli.ui.*

class Init : CliktCommand()
{
    override fun help(context: Context) = "Initialize modpack"

    private val nameOpt by option("-n", "--name")
        .help("Init the name of the modpack")

    private val versionOpt by option("-v", "--version")
        .help("Init the version of the modpack")

    private val mcVersionsOpts: List<String>? by option(
        "--mc-v", "--mc-versions",
        help = "Init the minecraft versions"
    ).varargValues()

    private val loadersOpts: Map<String, String>? by option(
        "-l", "--loaders",
        help = "Init the mod loaders",
        metavar = "<name>=<version>"
    ).associate()

    private val targetOpt: String? by option(
        "-t", "--target",
        help = "Init the target of the pack"
    ).choice("curseforge", "modrinth", "multiplatform", ignoreCase = true)

    override fun run() = runBlocking {
        if (LockFile.exists())
        {
            terminal.pDanger("Modpack is already initialized.")
            echo(
                hint(
                    "use \"pakku set\" or \"pakku cfg\" to change",
                    " already configured properties of your modpack"
                )
            )
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readOrNew()
        val lockFile = LockFile.readOrNew()

        /**
         * Set default export configuration for new projects.
         * New projects default to excluding server-side mods from client exports (correct behavior).
         * Existing projects maintain backward compatibility through the migration system.
         */
        configFile.setExportServerSideProjectsToClient(false)

        // -- NAME --

        with(nameOpt ?: terminal.prompt("? Modpack name") ?: "")
        {
            configFile.setName(this)
            terminal.success("'name' set to '$this'")
        }

        // -- VERSION --

        configFile.setVersion(versionOpt ?: "0.0.1")

        // -- MC VERSIONS --

        with(mcVersionsOpts ?: terminal.prompt("? Minecraft versions")?.split(" ") ?: return@runBlocking)
        {
            lockFile.setMcVersions(this)
            terminal.success("'mc_version' set to $this")
        }

        // -- LOADERS --

        if (loadersOpts.isNullOrEmpty())
        {
            with(terminal.prompt("? Loaders")?.split(" ") ?: return@runBlocking)
            {
                this.forEach { lockFile.addLoader(it, "") }
                terminal.success("'loaders' set to $this")
            }

            lockFile.getLoadersWithVersions().forEach { (loaderName, _) ->
                with(terminal.prompt("    ? $loaderName version") ?: "")
                {
                    lockFile.setLoader(loaderName, this)
                    terminal.success("    $loaderName version set to $this")
                }
            }
        }
        else
        {
            lockFile.setLoaders(loadersOpts!!)
        }

        // -- TARGET --

        with(
            targetOpt ?: runCatching {
                terminal.interactiveSelectList(
                    listOf("curseforge", "modrinth", "multiplatform"),
                    title = "? Target",
                )
            }
            .getOrElse {
                terminal.prompt("? Target", choices = listOf("curseforge", "modrinth", "multiplatform"))
            }
            ?: return@runBlocking
        )
        {
            lockFile.setTarget(this)
            terminal.success("'target' set to '$this'")
        }

        // -- OVERRIDES --

        configFile.addOverride("config")

        // -- FINISH --

        echo()

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
        configFile.write()?.onError {
            terminal.pError(it)
        }

        // -- API KEY --

        if (lockFile.getPlatforms().get()?.contains(CurseForge) == true
            && CredentialsFile.readToResult().get()?.curseForgeApiKey == null)
        {
            terminal.println("? CurseForge API key")
            echo()
            terminal.pMsg("Accessing CurseForge requires the CurseForge API key.")
            terminal.promptForCurseForgeApiKey()?.onError { error ->
                terminal.pError(error)
            }
        }

        terminal.pSuccess("Modpack successfully initialized")
        echo()
    }
}