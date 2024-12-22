package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
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

    override fun run() = runBlocking {
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

        val progressBar = progressBarLayout(spacing = 2) {
            spinner(Spinner.Dots())
        }.animateInCoroutine(terminal)

        launch { progressBar.execute() }

        val (addedProjects, removedProjects) = syncProjects(lockFile, configFile, platforms)

        progressBar.clear()

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
                onSuccess = { project, isRecommended, isReplacing, _ ->
                    val projMsg = project.getFullMsg()
                    val promptMessage = if (!isReplacing) "add" to "added" else "replace" to "replaced"

                    if (terminal.ynPrompt("Do you want to ${promptMessage.first} $projMsg?", isRecommended))
                    {
                        if (!isReplacing) lockFile.add(project) else lockFile.update(project)
                        lockFile.linkProjectToDependents(project)

                        terminal.pSuccess("$projMsg ${promptMessage.second}")

                        project.getSubpath()?.onSuccess { subpath ->
                            configFile?.setProjectConfig(projectIn, lockFile) { slug ->
                                this.subpath = subpath
                                terminal.pSuccess("'projects.$slug.subpath' set to '$subpath'")
                            }
                        }?.onFailure { error ->
                            terminal.pError(error)
                        }

                    }
                },
                lockFile, platforms
            )
        }

        removedProjects.takeIf { it.isNotEmpty() }?.run {
            if (addedProjects.isNotEmpty()) echo()

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
                    }
                },
                onDepRemoval = { _, _ ->

                },
                lockFile
            )
        }

        if (addedProjects.isNotEmpty() || removedProjects.isNotEmpty()) echo()

        lockFile.write()
        configFile?.write()
    }
}