package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.errors.AlreadyExists
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.remote.remoteInstall
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.readManualOverridesFrom
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.cli.ui.*
import teksturepako.pakku.integration.git.gitStatus
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds

class Remote : CliktCommand()
{
    override fun help(context: Context) = "Create and install modpacks from remote"

    private val urlArg: String? by argument("url")
        .help("URL of the remote package or Git repository")
        .optional()

    private val branchOpt: String? by option("-b", "--branch")
        .help("Checkout <branch> instead of the remote's HEAD")

    private val retryOpt: Int? by option("-r", "--retry", metavar = "<n>")
        .help("Retries downloading when it fails, with optional number of times to retry (Defaults to 2)")
        .int()
        .optionalValue(2)
        .default(0)

    private val serverPackFlag by option("-S", "--server-pack")
        .help("Install the server pack")
        .flag()

    data class Args(
        val urlArg: String?,
        val branchOpt: String?,
        val retryOpt: Int?,
        val serverPackFlag: Boolean
    )

    private val args by findOrSetObject { mutableListOf<Args>() }

    init
    {
        this.subcommands(RemoteUpdate(), RemoteRm())
    }

    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = false

    override fun run(): Unit = runBlocking {
        coroutineScope {
            // Pass args to the context
            args.clear()
            args += Args(urlArg, branchOpt, retryOpt, serverPackFlag)

            // Return if any subcommand is used.
            if (currentContext.invokedSubcommands.isNotEmpty()) return@coroutineScope

            suspend fun install(url: String, branch: String?)
            {
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
                    remoteInstall(
                        onProgress = { taskName, percentDone ->
                            val id = taskName?.lowercase()?.filterNot { it.isWhitespace() }
                            if (id != null)
                            {
                                // Start progress animation when first task is added
                                if (tasks.value.isEmpty()) {
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
                        url, branch, if (serverPackFlag) setOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE) else null
                    )
                }

                remoteJob.await()?.onError {
                    terminal.pError(it)
                    echo()
                    return
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
                        return
                    }

                val configFile = if (ConfigFile.existsAt(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME)))
                {
                    ConfigFile.readToResultFrom(Path(Dirs.remoteDir.pathString, ConfigFile.FILE_NAME))
                        .getOrElse {
                            terminal.pError(it)
                            echo()
                            return
                        }
                }
                else null

                val projectFiles = retrieveProjectFiles(
                    lockFile, Provider.providers,
                    if (serverPackFlag) setOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE) else null
                ).mapNotNull { result ->
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
                    lockFile, configFile, retryOpt
                )

                // -- OVERRIDES --

                val projectOverrides = readManualOverridesFrom(
                    Dirs.remoteDir, configFile,
                    if (serverPackFlag) setOf(OverrideType.OVERRIDE, OverrideType.SERVER_OVERRIDE) else null
                )

                val syncJob = launch {
                    projectOverrides.sync(
                        onError = { error ->
                            if (error !is AlreadyExists) terminal.pError(error)
                        },
                        onSuccess = { projectOverride ->
                            terminal.pInfo("${projectOverride.fullOutputPath} synced")
                        },
                        syncPrimaryDirectories = true
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

            if (urlArg != null)
            {
                install(urlArg!!, branchOpt)
            }
            else
            {
                val (status, repo) = gitStatus(Dirs.remoteDir).get()
                    ?: return@coroutineScope

                echo("On branch ${repo.branch}")

                if (status.isClean)
                {
                    terminal.pSuccess("Your branch is up to date")
                }
                else
                {
                    terminal.pMsg("There are changes on the remote")
                }

                echo()
            }
        }
    }
}
