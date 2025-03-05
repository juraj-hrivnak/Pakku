package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.terminal.prompt
import com.github.ajalt.mordant.terminal.success
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.cli.arg.promptForCurseForgeApiKey
import teksturepako.pakku.cli.ui.hint
import teksturepako.pakku.cli.ui.pDanger

class Init : CliktCommand()
{
    override fun help(context: Context) = "Initialize modpack"

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

        // -- NAME --

        with(terminal.prompt("? Modpack name") ?: "")
        {
            configFile.setName(this)
            terminal.success("'name' set to '$this'")
        }

        // -- VERSION --

        configFile.setVersion("0.0.1")

        // -- MC VERSIONS --

        with(terminal.prompt("? Minecraft versions")?.split(" ") ?: return@runBlocking)
        {
            lockFile.setMcVersions(this)
            terminal.success("'mc_version' set to $this")
        }

        // -- LOADERS --

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

        // -- TARGET --

        with(
            runCatching {
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

        if (lockFile.getPlatforms().get()?.contains(CurseForge) == true)
        {
            terminal.println("? CurseForge API key")
            terminal.promptForCurseForgeApiKey()
        }

        // -- OVERRIDES --

        configFile.addOverride("config")

        // -- FINISH --

        echo()

        lockFile.write()
        configFile.write()
        terminal.success("Modpack successfully initialized")
        echo()
    }
}