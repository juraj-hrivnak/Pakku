package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.colormath.Color
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.ajalt.mordant.widgets.progress.text
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.fetch.DeletionActionType
import teksturepako.pakku.api.actions.fetch.deleteOldFiles
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.cli.ui.*
import kotlin.io.path.Path
import kotlin.io.path.pathString

class Fetch : CliktCommand()
{
    override fun help(context: Context) = "Download project files to your modpack folder"

    private val retryOpt: Int? by option("-r", "--retry", metavar = "<n>")
        .help("Retries downloading when it fails, with optional number of times to retry (Defaults to 2)")
        .int()
        .optionalValue(2)
        .default(0)

    private val shelveFlag: Boolean by option("--shelve")
        .help("Moves unknown project files to a shelf instead of deleting them")
        .flag()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pError(it)
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

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

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
            text { context }
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
            onSuccess = { path, projectFile ->
                val slug = projectFile.getParentProject(lockFile)?.getFullMsg()

                terminal.pSuccess("$slug saved to $path")
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

        syncJob.invokeOnCompletion {
            progressBar.update {
                context = fetchMsg("Fetched", RGB("#98c379"))
                paused = true
            }

            runBlocking {
                progressBar.stop()
            }
        }

        // -- OLD FILES --

        val oldFilesJob = launch {
            deleteOldFiles(
                onError = { error ->
                    terminal.pError(error)
                },
                onSuccess = { file, actionType ->
                    when (actionType)
                    {
                        DeletionActionType.DELETE -> terminal.pDanger("$file ${actionType.result}")
                        DeletionActionType.SHELF  ->
                        {
                            val shelvedPath = Path(Dirs.shelfDir.pathString, file.fileName.pathString)
                            terminal.pInfo("$file ${actionType.result} to $shelvedPath")
                        }
                    }
                },
                projectFiles, projectOverrides, lockFile, configFile, platforms, shelveFlag
            )
        }

        fetchJob.join()
        syncJob.join()
        oldFilesJob.join()

        echo()
    }
}