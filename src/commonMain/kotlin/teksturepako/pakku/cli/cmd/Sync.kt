package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.actions.sync.syncProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.*

class Sync : CliktCommand()
{
    override fun help(context: Context) = "Sync your modpack with local project files"

    private val additionsFlag by option("-A", "--additions").flag()
        .help("Sync additions only")

    private val removalsFlag by option("-R", "--removals").flag()
        .help("Sync removals only")

    private val updatesFlag by option("-U", "--updates").flag()
        .help("Sync updates only")

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            it.message?.let { message -> terminal.pDanger(message) }
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
            it.message?.let { message -> terminal.pDanger(message) }
            echo()
            return@runBlocking
        }

        val flagsUsed = additionsFlag || removalsFlag || updatesFlag

        val progressBar = progressBarLayout(spacing = 2) {
            spinner(Spinner.Dots())
        }.animateInCoroutine(terminal)

        launch { progressBar.execute() }

        val (addedProjects, removedProjects, updatedProjects) = syncProjects(lockFile, configFile, platforms)

        progressBar.clear()

        // -- ADDITIONS --

        if (!flagsUsed || additionsFlag)
        {
            addedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = addedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (addedProjects.size > 1) "were" else "was"

                echo("$msg $verb added to your modpack's file system.")
                echo()
            }

            for (projectIn in addedProjects)
            {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        terminal.pError(error)
                    },
                    onSuccess = { project, isRecommended, replacing, _ ->
                        val projMsg = project.getFullMsg()
                        val promptMessage = if (replacing == null)
                        {
                            "Do you want to add $projMsg?" to "$projMsg added"
                        }
                        else
                        {
                            val replacingMsg = replacing.getFullMsg()
                            "Do you want to replace $replacingMsg with $projMsg?" to
                                    "$replacingMsg replaced with $projMsg"
                        }

                        if (terminal.ynPrompt(promptMessage.first, isRecommended))
                        {
                            if (replacing == null) lockFile.add(project) else lockFile.update(project)
                            lockFile.linkProjectToDependents(project)

                            terminal.pSuccess(promptMessage.second)
                        }
                    },
                    lockFile, platforms
                )
            }

            if (addedProjects.isNotEmpty()) echo()
        }

        // -- REMOVALS --

        if (!flagsUsed || removalsFlag)
        {
            removedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = removedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (removedProjects.size > 1) "were" else "was"

                echo("$msg $verb removed from your modpack's file system.")
                echo()
            }

            for (projectIn in removedProjects)
            {
                projectIn.createRemovalRequest(
                    onError = { error ->
                        terminal.pError(error)
                    },
                    onRemoval = { project, isRecommended ->
                        val projMsg = project.getFullMsg()

                        if (terminal.ynPrompt("Do you want to remove $projMsg?", isRecommended))
                        {
                            lockFile.remove(project)
                            lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                            terminal.pDanger("$projMsg removed")

                            project.getSubpath()?.onSuccess {
                                configFile?.setProjectConfig(projectIn, lockFile) { slug ->
                                    this.subpath = null
                                    terminal.pDanger("'projects.$slug.subpath' removed")
                                }
                            }?.onFailure { error ->
                                terminal.pError(error)
                            }
                        }
                    },
                    onDepRemoval = { _, _ ->

                    },
                    lockFile
                )
            }

            if (removedProjects.isNotEmpty()) echo()
        }

        if (!flagsUsed || updatesFlag)
        {
            updatedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = updatedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (updatedProjects.size > 1) "were" else "was"

                echo("$msg $verb updated in your modpack's file system.")
                echo()
            }

            for (project in updatedProjects)
            {
                if (terminal.ynPrompt("Do you want to update ${project.getFullMsg()}?", true))
                {
                    lockFile.update(project)
                    terminal.pInfo("${project.getFullMsg()} updated")

                    project.getSubpath()?.onSuccess { subpath ->
                        configFile?.setProjectConfig(project, lockFile) { slug ->
                            this.subpath = subpath
                            terminal.pInfo("'projects.$slug.subpath' set to '$subpath'")
                        }
                    }?.onFailure { error ->
                        terminal.pError(error)
                    }
                }
            }

            if (updatedProjects.isNotEmpty()) echo()
        }

        lockFile.write()?.let { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
        configFile?.write()?.let { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
    }
}
