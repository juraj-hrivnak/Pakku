package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.export
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.ProjectSide


class Export : CliktCommand("Export modpack")
{
    private val pathArg by argument("path").optional()
    private val sideOpt: String by option(
        "-s", "--side",
        help = "The side of the projects to export only"
    ).choice("client", "server", "both", ignoreCase = true).default("both")

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
            pathArg, ProjectSide.valueOf(sideOpt.uppercase()), lockFile, configFile, platforms
        )

        echo()
    }
}