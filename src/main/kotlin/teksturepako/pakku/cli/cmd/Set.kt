package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock

class Set : CliktCommand("Set pack name, Minecraft versions and loaders")
{
    private val packNameOpt: String? by option("-n", "--packname", help = "Change the name of the pack")
    private val mcVersionsOpts: List<String>? by option("-v", "--mcversion", help = "Change the minecraft version").varargValues()
    private val loadersOpts: List<String>? by option("-l", "--loader", help = "Change the mod loader").varargValues()

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        packNameOpt?.let {
            terminal.success("\"pack_name\" set to $it")
            pakkuLock.setPackName(it)
        }
        mcVersionsOpts?.let { versions ->
            var failed = false

            pakkuLock.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.mcVersions.none { it in versions })
                    {
                        terminal.danger(
                            "Can not set to $versions," + " because ${project.name.values.first()} (${file.type}) requires ${file.mcVersions}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                pakkuLock.setMcVersion(versions)
                terminal.success("\"mc_version\" set to $versions")
            }
        }
        loadersOpts?.let { loaders ->
            var failed = false

            pakkuLock.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.loaders.none { it in loaders })
                    {
                        terminal.danger(
                            "Can not set to $loaders," + " because ${project.name.values.first()} (${file.type}) requires ${file.loaders}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                terminal.success("\"loaders\" set to $loaders")
                pakkuLock.setModLoader(loaders)
            }
        }

        pakkuLock.write()
        echo()
    }
}