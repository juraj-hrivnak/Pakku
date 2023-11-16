package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.data.PakkuLock

class Set : CliktCommand("Set packname, mcversion and loader")
{
    private val packName: String? by option("-n", "--packname", help="Change the name of the pack")
    private val mcVersion: String? by option("-v", "--mcversion", help="Change the minecraft version")
    private val loader: String? by option("-l", "--loader", help="Change the mod loader")

    override fun run() = runBlocking {
        packName?.let {
            terminal.success("packname set to $it")
            PakkuLock.setPackName(it)
        }
        mcVersion?.let {
            terminal.success("mcversion set to $it")
            PakkuLock.setMcVersion(it)
        }
        loader?.let {
            terminal.success("loader set to $it")
            PakkuLock.setModLoader(it)
        }
        echo()
    }
}