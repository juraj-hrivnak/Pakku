package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.export
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.platforms.Platform


class Export : CliktCommand("Export modpack")
{
    private val pathArg by argument("path").optional()

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

        val projectOverrides = Overrides.getProjectOverrides()

        export(
            onSuccess = { terminal.success(it) },
            onError = { terminal.danger(it) },
            onWarning = { terminal.warning(it) },
            onInfo = { terminal.println(it) },
            pathArg, lockFile, configFile, projectOverrides.toMutableList(), platforms
        )

        echo()
    }
}