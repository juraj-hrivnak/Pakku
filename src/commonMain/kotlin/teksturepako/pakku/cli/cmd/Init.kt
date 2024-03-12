package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile

class Init : CliktCommand("Initialize modpack")
{
    override fun run() = runBlocking {
        if (LockFile.exists())
        {
            terminal.danger("Modpack is already initialized")
            terminal.info("To change already initialized properties, use the command: \"set\"")
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
            terminal.prompt("? Target", choices = listOf("curseforge", "modrinth", "multiplatform"))
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

        lockFile.write()
        configFile.write()
        terminal.success("Modpack successfully initialized")
        echo()
    }
}