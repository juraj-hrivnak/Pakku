package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.export.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.ui.prefixed
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

        export(
            profiles = listOf(
                ExportProfile(
                    name = CurseForge.serialName,
                    rules = listOf(
                        lockFile.getFirstMcVersion()?.let {
                            createCfModpackModel(it, lockFile, configFile)
                        }?.let { exportCurseForge(it) },
                        if (lockFile.getAllProjects().any { "filedirector" in it })
                        {
                            exportFileDirector()
                        }
                        else
                        {
                            exportMissingProjects(Modrinth)
                        }
                    )
                ),
                ExportProfile(
                    name = "serverpack",
                    rules = listOf(
                        exportServerPack()
                    )
                )
            ),
            onError = { error ->
                if (error !is AlreadyExists) terminal.println(processErrorMsg(error))
            },
            onSuccess = { profile, file ->
                terminal.success(prefixed("${profile.name} exported to '$file'"))
                echo()
            },
            lockFile, configFile
        )
    }
}