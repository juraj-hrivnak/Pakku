package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.errors.ErrorWhileExporting
import teksturepako.pakku.api.actions.export.exportDefaultProfiles
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

        val progressBar = progressBarLayout(spacing = 2) {
            spinner(Spinner.Dots())
        }.animateInCoroutine(terminal)

        launch { progressBar.execute() }

        exportDefaultProfiles(
            onError = { profile, error ->
                if (error !is ErrorWhileExporting)
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

        progressBar.clear()

        echo()
    }
}