package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import kotlinx.coroutines.runBlocking
import teksturepako.data.PakkuLock

class Set : CliktCommand("Set packname, mcversion and loader")
{
    private val packName: String? by option("-n", "--packname", help="Change the name of the pack")
    private val mcVersions: List<String>? by option("-v", "--mcversion", help="Change the minecraft version").varargValues()
    private val loaders: List<String>?  by option("-l", "--loader", help="Change the mod loader").varargValues()

    override fun run() = runBlocking {
        packName?.let {
            terminal.success("\"pack_name\" set to $it")
            PakkuLock.setPackName(it)
        }
        mcVersions?.let { versions ->
            var failed = false

            PakkuLock.get { data ->
                for (project in data.projects)
                {
                    for (file in project.files) {
                        if (file.mcVersions.none { it in versions }) {
                            terminal.danger("Can not set to $versions," +
                                    " because ${project.name.values.first()} (${file.type}) requires ${file.mcVersions}")
                            failed = true
                        }
                    }
                }
            }

            if (!failed) {
                PakkuLock.setMcVersion(versions)
                terminal.success("\"mc_version\" set to $versions")
            }
        }
        loaders?.let { loaders ->
            var failed = false

            PakkuLock.get { data ->
                for (project in data.projects)
                {
                    for (file in project.files) {
                        if (file.loaders.none { it in loaders }) {
                            terminal.danger("Can not set to $loaders," +
                                    " because ${project.name.values.first()} (${file.type}) requires ${file.loaders}")
                            failed = true
                        }
                    }
                }
            }

            if (!failed) {
                terminal.success("\"loaders\" set to $loaders")
                PakkuLock.setModLoader(loaders)
            }
        }
        echo()
    }
}