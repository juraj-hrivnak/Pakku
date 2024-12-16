package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.export.export
import teksturepako.pakku.api.actions.export.profiles.CurseForgeProfile
import teksturepako.pakku.api.actions.export.profiles.ModrinthProfile
import teksturepako.pakku.api.actions.export.profiles.ServerPackProfile
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import kotlin.io.path.fileSize

class Export : CliktCommand()
{
    override fun help(context: Context) = "Export modpack"

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = ConfigFile.readToResult().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
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
                {
                    terminal.pError(error, prepend = "[${profile.name} profile]")
                }
            },
            onSuccess = { profile, file, duration ->
                val fileSize = file.fileSize().toHumanReadableSize()

                terminal.pSuccess("[${profile.name} profile] exported to '$file' ($fileSize) in ${duration.shortForm()}")
            },
            lockFile, configFile, platforms
        ).joinAll()

        echo()
    }
}