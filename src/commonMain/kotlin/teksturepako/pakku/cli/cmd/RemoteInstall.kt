package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.remote.remoteInstall
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.cli.ui.*
import kotlin.time.Duration.Companion.seconds

class RemoteInstall : CliktCommand("install")
{
    override fun help(context: Context) = "install modpacks from remote"

    private val args by requireObject<MutableList<Remote.Args>>()

    private val retryOpt: Int? by option("-r", "--retry", metavar = "<n>")
        .help("Retries downloading when it fails, with optional number of times to retry (Defaults to 2)")
        .int()
        .optionalValue(2)
        .default(0)

    override fun run() = runBlocking {
        coroutineScope {

            val (url, branch) = args.first()

            terminal.cursor.hide()

            val gitProgressLayout = progressBarContextLayout(spacing = 2, animationFps = 10) {
                text(align = TextAlign.LEFT) {
                    prefixed(context, prefix = terminal.theme.string("pakku.prefix", ">>>"))
                }
                percentage()
                progressBar()
            }

            val gitProgress = MultiProgressBarAnimation(terminal).animateInCoroutine()
            val tasks = mutableListOf<ProgressTask<String>>()

            val remoteJob = async {
                remoteInstall(url, branch) { (taskName, percentDone) ->
                    if (taskName != null && taskName !in tasks.map { it.context })
                    {
                        tasks += gitProgress.addTask(gitProgressLayout, taskName, total = 100)
                    }

                    for (task in tasks)
                    {
                        if (task.context == taskName) runBlocking {
                            task.update {
                                this.completed = percentDone.toLong()
                            }
                        }
                    }

                    launch {
                        gitProgress.refresh()
                    }
                }
            }

//            launch { gitProgress.execute() }

            remoteJob.await()?.onError {
                terminal.pError(it)
                echo()
                return@coroutineScope
            }

            remoteJob.join()

            launch {
                delay(1.seconds)
                for (task in tasks)
                {
                    task.update {
                        this.completed = 100
                    }
                }
                runBlocking {
                    gitProgress.stop()
                }
            }.join()

            terminal.cursor.show()

            val lockFile = LockFile.readToResult().getOrElse {
                terminal.pError(it)
                echo()
                return@coroutineScope
            }

            val configFile = if (ConfigFile.exists())
            {
                ConfigFile.readToResult().getOrElse {
                    terminal.pError(it)
                    echo()
                    return@coroutineScope
                }
            }
            else null

            val projectFiles = retrieveProjectFiles(lockFile, Provider.providers).mapNotNull { result ->
                result.getOrElse {
                    terminal.pError(it)
                    null
                }
            }

            val progressBar = progressBarContextLayout(spacing = 2) {
                text {
                    prefixed(context, prefix = terminal.theme.string("pakku.prefix", ">>>"))
                }
                percentage()
                progressBar()
            }.animateInCoroutine(terminal, "Fetching")

            launch { progressBar.execute() }

            val fetchJob = projectFiles.fetch(onError = { error ->
                if (error !is AlreadyExists) terminal.pError(error)
            }, onProgress = { completed, total ->
                progressBar.update {
                    this.completed = completed
                    this.total = total
                }
            }, onSuccess = { path, projectFile ->
                val slug = projectFile.getParentProject(lockFile)?.getFullMsg()

                terminal.pSuccess("$slug saved to $path")
            }, lockFile, configFile, retryOpt
            )

            // -- OVERRIDES --

            val projectOverrides = readProjectOverrides(configFile)

            val syncJob = launch {
                projectOverrides.sync(onError = { error ->
                    if (error !is AlreadyExists) terminal.pError(error)
                }, onSuccess = { projectOverride ->
                    terminal.pInfo("${projectOverride.fullOutputPath} synced")
                })
            }

            fetchJob.join()

            launch {
                delay(1.seconds)
                progressBar.update {
                    if (this.total != null)
                    {
                        this.completed = this.total!!
                    }
                }
                runBlocking {
                    progressBar.stop()
                }
            }.join()

            syncJob.join()

            echo()
        }
    }
}