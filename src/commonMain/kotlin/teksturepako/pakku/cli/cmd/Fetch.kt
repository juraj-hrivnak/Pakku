package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.*
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
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pInfo
import teksturepako.pakku.cli.ui.pSuccess

class Fetch : CliktCommand()
{
    override fun help(context: Context) = "Fetch projects to your modpack folder"

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val configFile = if (ConfigFile.exists())
        {
            ConfigFile.readToResult().getOrElse {
                terminal.danger(it.message)
                echo()
                return@runBlocking
            }
        }
        else null

        val progressBar = progressBarContextLayout(spacing = 0) {
            text { context.toString() }
            spinner(Spinner.Dots())
            percentage()
        }.animateInCoroutine(terminal, "Fetching ")

        launch { progressBar.execute() }

        val projectFiles = retrieveProjectFiles(lockFile, Provider.providers).mapNotNull { result ->
            result.getOrElse {
                terminal.pError(it)
                null
            }
        }

        val fetchJob = launch {
            projectFiles.fetch(
                onError = { error ->
                    if (error !is AlreadyExists) terminal.pError(error)
                },
                onProgress = { advance, total ->
                    progressBar.advance(advance)
                    progressBar.update { this.total = total }
                },
                onSuccess = { path, _ ->
                    terminal.pSuccess("$path saved")
                },
                lockFile, configFile
            )
        }

        fetchJob.invokeOnCompletion {
            progressBar.update {
                context = "Fetched "
            }
            launch {
                progressBar.stop()
            }
        }

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

        // -- OLD FILES --

        val oldFilesJob = launch {
            deleteOldFiles(
                onSuccess = { file ->
                    terminal.pDanger("$file deleted")
                },
                projectFiles, projectOverrides, configFile
            )
        }

        syncJob.join()
        oldFilesJob.join()

        echo()
    }
}