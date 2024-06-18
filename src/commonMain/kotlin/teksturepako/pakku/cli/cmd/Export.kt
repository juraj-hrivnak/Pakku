package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.export.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.processErrorMsg
import teksturepako.pakku.compat.exportFileDirector
import teksturepako.pakku.io.tryOrNull
import kotlin.io.path.*


class Export : CliktCommand("Export modpack")
{
    private val pathArg by argument("path").optional()
    private val serverPackFlag: Boolean by option(
        "-s","--server-pack",
        help = "Export Server-Pack only"
    ).flag()

    @OptIn(ExperimentalPathApi::class)
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

        val modpackModel = createCfModpackModel(
            lockFile.getFirstMcVersion() ?: return@runBlocking,
            lockFile, configFile
        )

        for (platform in platforms)
        {
            platform.export(
                rules = listOf(
                    ExportRule { entry ->
                        val overrides = configFile.getAllOverrides()

                        overrides.forEach { override ->
                            val file = Path(workingPath, override)

                            if (file.isRegularFile())
                            {
                                val outputFile = Path(workingPath, PAKKU_DIR, "temp", "overrides", override)
                                file.tryOrNull { it.copyTo(outputFile) }
                            }
                            else if (file.isDirectory())
                            {
                                val outputFile = Path(workingPath, PAKKU_DIR, "temp", "overrides", override)
                                file.tryOrNull { it.copyToRecursively(outputFile, followLinks = true) }
                            }
                        }

                        entry.ignore()
                    },
                    exportCurseForge(modpackModel),
                    exportFileDirector()
                ),
                onError = { error ->
                    terminal.println(processErrorMsg(error))
                },
                lockFile, configFile
            )
        }
    }
}