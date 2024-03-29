package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.export
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform


class Export : CliktCommand("Export modpack")
{
    private val pathArg by argument("path").optional()
    private val serverPackFlag: Boolean by option(
        "-s","--server-pack",
        help = "Export Server-Pack only"
    ).flag()

    override fun run() = runBlocking {
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

        export(
            onSuccess = { terminal.success(it) },
            onError = { terminal.danger(it) },
            onWarning = { terminal.warning(it) },
            onInfo = { terminal.println(it) },
            pathArg, serverPackFlag, lockFile, configFile, platforms
        )
    }
}