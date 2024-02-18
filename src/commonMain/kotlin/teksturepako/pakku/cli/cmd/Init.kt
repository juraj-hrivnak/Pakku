package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock

class Init : CliktCommand("Initialize modpack")
{
    override fun run() = runBlocking {
        if (PakkuLock.exists())
        {
            terminal.danger("Modpack is already initialized")
            terminal.info("To change already initialized properties, use the command: \"set\"")
            echo()
            return@runBlocking
        }

        val pakkuLock = PakkuLock.readOrNew()

        val packName: String =
            terminal.prompt("? Modpack name") ?: ""

        pakkuLock.setPackName(packName)
        terminal.success("'name' set to '$packName'")

        val mcVersions: List<String> =
            terminal.prompt("? Minecraft versions")?.split(" ") ?: return@runBlocking

        pakkuLock.setMcVersions(mcVersions)
        terminal.success("'mc_version' set to $mcVersions")

        val loaders: List<String> =
            terminal.prompt("? Loaders")?.split(" ") ?: return@runBlocking

        pakkuLock.setModLoaders(loaders)
        terminal.success("'loaders' set to $loaders")

        val target: String =
            terminal.prompt("? Target", choices = listOf("curseforge", "modrinth", "multiplatform"))
                ?: return@runBlocking

        pakkuLock.setPlatformTarget(target)
        terminal.success("'target' set to '$target'")

        echo()

        pakkuLock.write()
        terminal.success("Modpack successfully initialized")
        echo()
    }
}