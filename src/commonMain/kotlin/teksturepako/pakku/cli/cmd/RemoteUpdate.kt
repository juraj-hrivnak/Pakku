package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.michaelbull.result.getOrElse
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.remote.remoteUpdate
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.readProjectOverridesFrom
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.cli.ui.*
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds

class RemoteUpdate : CliktCommand("update")
{
    override fun help(context: Context) = "update the modpack from its remote"

    private val argObjects by requireObject<MutableList<Remote.Args>>()

    override fun run() = runBlocking {
        coroutineScope x@ {
            val args = argObjects.first()

            terminal.cursor.hide()

            val gitProgressLayout = progressBarContextLayout(spacing = 2) {
                text(align = TextAlign.LEFT) {
                    prefixed(context, prefix = terminal.theme.string("pakku.prefix", ">>>"))
                }
                percentage()
                progressBar()
            }

            val gitProgress = MultiProgressBarAnimation(terminal).animateInCoroutine()
            val tasks = atomic(mutableMapOf<String, ProgressTask<String>>())

            val remoteJob = async {
                remoteUpdate(
                    onProgress = { taskName, percentDone ->
                        val id = taskName?.lowercase()?.filterNot { it.isWhitespace() }
                        if (id != null)
                        {
                            // Start progress animation when first task is added
                            if (tasks.value.isEmpty())
                            {
                                launch { gitProgress.execute() }
                            }

                            // Atomically update tasks map
                            tasks.update { currentTasks ->
                                if (id !in currentTasks)
                                {
                                    currentTasks[id] = gitProgress.addTask(gitProgressLayout, taskName, total = 100)
                                    currentTasks
                                }
                                else
                                {
                                    currentTasks
                                }
                            }

                            // Update the task progress
                            tasks.value[id]?.let { task ->
                                runBlocking {
                                    task.update {
                                        this.completed = percentDone.toLong()
                                    }
                                }
                            }
                        }
                    },
                    onSync = {
                        terminal.pSuccess(it.description)
                    },
                )
            }

            remoteJob.await()?.onError {
                terminal.pError(it)
                echo()
                return@x
            }

            remoteJob.join()

            launch {
                delay(1.seconds)
                runBlocking {
                    gitProgress.stop()
                }
            }.join()

            terminal.cursor.show()

            val lockFile = LockFile.readToResultFrom(Path(Dirs.remoteDir.pathString, LockFile.FILE_NAME))
                .getOrElse {
                    terminal.pError(it)
                    echo()
                    return@x
                }

            val configFile = if (ConfigFile.existsAt(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)))
            {
                ConfigFile.readToResultFrom(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME))
                    .getOrElse {
                        terminal.pError(it)
                        echo()
                        return@x
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
                lockFile, configFile, args.retryOpt
            )

            // -- OVERRIDES --

            val projectOverrides = readProjectOverridesFrom(Dirs.remoteDir, configFile)

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

            launch {
                delay(3.seconds)
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
