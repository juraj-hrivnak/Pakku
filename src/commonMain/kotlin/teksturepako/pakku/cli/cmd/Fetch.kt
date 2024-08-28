package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.widgets.Spinner
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.fetch.deleteOldFiles
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pInfo
import teksturepako.pakku.cli.ui.pSuccess

class Fetch : CliktCommand("Fetch projects to your modpack folder")
{
    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val progressBar = terminal.progressAnimation {
            text("Fetching ")
            spinner(Spinner.Dots())
            percentage()
            padding = 0
        }

        val projectFiles = retrieveProjectFiles(lockFile, Multiplatform.platforms).mapNotNull { result ->
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
                    progressBar.updateTotal(total)
                },
                onSuccess = { projectFile, _ ->
                    terminal.pSuccess("${projectFile.getPath()} saved")
                },
                lockFile
            )
        }

        fetchJob.invokeOnCompletion {
            progressBar.clear()
        }

        // -- OVERRIDES --

        val projectOverrides = readProjectOverrides()

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

        // -- OLD FILES --

        val oldFilesJob = launch {
            deleteOldFiles(
                onSuccess = { file ->
                    terminal.pDanger("$file deleted")
                },
                projectFiles, projectOverrides
            )
        }

        fetchJob.join()
        syncJob.join()
        oldFilesJob.join()

        echo()
    }
}