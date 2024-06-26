package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.export.*
import teksturepako.pakku.api.actions.export.profiles.CurseForgeProfile
import teksturepako.pakku.api.actions.export.profiles.ModrinthProfile
import teksturepako.pakku.api.actions.export.profiles.ServerPackProfile
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.prefixed
import teksturepako.pakku.cli.ui.processErrorMsg

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
                CurseForgeProfile(lockFile, configFile),
                ModrinthProfile(lockFile, configFile),
                ServerPackProfile()
            ),
            onError = { profile, error ->
                if (error !is AlreadyExists)
                    terminal.println(processErrorMsg(error, prefix = profile.name))
            },
            onSuccess = { profile, file ->
                terminal.success(prefixed("${profile.name} exported to '$file'"))
            },
            lockFile, configFile
        )
    }
}