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
            lockFile.setName(this)
            terminal.success("'name' set to '$this'")
        }

        // -- VERSION --

        with(terminal.prompt("? Modpack version") ?: "0.0.1")
        {
            configFile.version = this
            terminal.success("'version' set to $this")
        }

        // -- MC VERSIONS --

        with(terminal.prompt("? Minecraft versions")?.split(" ") ?: return@runBlocking)
        {
            lockFile.setMcVersions(this)
            terminal.success("'mc_version' set to $this")
        }

        // -- LOADERS --

        with(terminal.prompt("? Loaders")?.split(" ") ?: return@runBlocking)
        {
            lockFile.setLoaders(this)
            this.forEach { configFile.loaders[it] = "" }
            terminal.success("'loaders' set to $this")
        }

        configFile.loaders.forEach { (loader, _) ->
            with(terminal.prompt("? $loader version") ?: "")
            {
                configFile.loaders[loader] = this
                terminal.success("$loader version set to $this")
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

        // -- FINISH --

        echo()

        lockFile.write()
        configFile.write()
        terminal.success("Modpack successfully initialized")
        echo()
    }
}