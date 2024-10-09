package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.mordant.terminal.danger
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.export
import teksturepako.pakku.api.actions.export.profiles.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.arg.ProjectArg
import teksturepako.pakku.cli.arg.mapProjectArg
import teksturepako.pakku.cli.ui.createHyperlink
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import teksturepako.pakku.io.tryOrNull
import kotlin.io.path.absolutePathString
import kotlin.io.path.fileSize

class Export : CliktCommand()
{
    private val profiles: List<String> by argument(
        name = "profiles",
        help = "Profiles to export. Will export server pack, mrpack and CurseForge pack if empty"
    ).multiple()

    override fun help(context: Context) = "Export modpack"

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

        val profiles = profiles.mapNotNull {
            (ExportProfile.all.getOrElse(it) {
                terminal.pError(ActionError("Can't find export profile '$it'"))
                null
            })?.invoke(lockFile, configFile)
        }.let {
            if (it.isEmpty())
            {
                listOf(
                    CurseForgeProfile(lockFile, configFile),
                    ModrinthProfile(lockFile, configFile),
                    ServerPackProfile(),
                )
            }
            it
        }

        export(
            profiles = profiles,
            onError = { profile, error ->
                if (error !is AlreadyExists)
                {
                    terminal.pError(error, prepend = "[${profile.name} profile]")
                }
            },
            onSuccess = { profile, file, duration ->
                val fileSize = file.fileSize().toHumanReadableSize()
                val filePath = file.tryOrNull { it.absolutePathString() }
                    ?.let { file.toString().createHyperlink(it) } ?: file.toString()

                terminal.pSuccess("[${profile.name} profile] exported to '$filePath' ($fileSize) in ${duration.shortForm()}")
            },
            lockFile, configFile, platforms
        ).joinAll()

        echo()
    }
}