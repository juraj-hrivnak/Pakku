package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.ajalt.mordant.widgets.progress.text
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.fetch.deleteOldFiles
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.cli.ui.*

class Fetch : CliktCommand()
{
    override fun help(context: Context) = "Fetch projects to your modpack folder"

    private val retryOpt: Int? by option("--retry")
        .help("The number of times to retry")
        .int()
        .optionalValue(2)
        .default(0)

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = if (ConfigFile.exists())
        {
            ConfigFile.readToResult().getOrElse {
                terminal.pError(it)
                echo()
                return@runBlocking
            }
        }
        else null

        val projectFiles = retrieveProjectFiles(lockFile, Provider.providers).mapNotNull { result ->
            result.getOrElse {
                terminal.pError(it)
                null
            }
        }

        fun fetchMsg(text: String, color: Color? = null) = TextStyle(color = color)(
            prefixed(text, prefix = terminal.theme.string("pakku.prefix", ">>>"))
        )

        val progressBar = progressBarContextLayout(spacing = 2) {
            text { context.toString() }
            spinner(Spinner.Dots())
            percentage()
        }.animateInCoroutine(terminal, fetchMsg("Fetching"))

        launch { progressBar.execute() }

        val fetchJob = projectFiles.fetch(
            onError = { error ->
                if (error !is AlreadyExists) terminal.pError(error)
            },
            onProgress = { completed, total ->
                progressBar.update {
                    this.completed = completed
                    this.total = total
                }
            },
            onSuccess = { path, _ ->
                terminal.pSuccess("$path saved")
            },
            lockFile, configFile, retryOpt
        )

        // -- OVERRIDES --

        val projectOverrides = readProjectOverrides(configFile)

        val syncJob = launch {
            projectOverrides.sync(
                onError = { error ->
                    if (error !is AlreadyExists) terminal.pError(error)
                },
                onSuccess = { projectOverride ->
                    terminal.pInfo("${projectOverride.fullOutputPath} synced")
                }
            )
        }

        fetchJob.join()

        progressBar.update {
            context = fetchMsg("Fetched", RGB("#98c379"))
            paused = true
        }

        // -- OLD FILES --

        val oldFilesJob = launch {
            deleteOldFiles(
                onError = { error ->
                    terminal.pError(error)
                },
                onSuccess = { file ->
                    terminal.pDanger("$file deleted")
                },
                projectFiles, projectOverrides, lockFile, configFile
            )
        }

        syncJob.join()
        oldFilesJob.join()

        progressBar.stop()

        echo()
    }
}