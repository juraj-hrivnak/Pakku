package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.export.createCfModpackModel
import teksturepako.pakku.api.actions.export.export
import teksturepako.pakku.api.actions.export.exportCurseForge
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.processErrorMsg
import teksturepako.pakku.compat.exportFileDirector

class Export : CliktCommand("Export modpack")
{
    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val modpackModel = lockFile.getFirstMcVersion()?.let {
            createCfModpackModel(it, lockFile, configFile)
        }

        export(
            rules = listOf(
                modpackModel?.let { exportCurseForge(it) },
                exportFileDirector(),
            ),
            onError = { error ->
                terminal.println(processErrorMsg(error))
            },
            lockFile, configFile
        )
    }
}