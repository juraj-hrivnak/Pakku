package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyAdded
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.import.importModpackModel
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import teksturepako.pakku.cli.ui.promptForProject

class Import : CliktCommand("Import modpack")
{
    private val pathArg: String by argument("path")
    private val depsFlag: Boolean by option("-D", "--deps", help = "Resolve dependencies").flag()

    override fun run() = runBlocking {
        val modpackModel = importModpackModel(pathArg).getOrElse {
            terminal.pError(it, pathArg)
            echo()
            return@runBlocking
        }

        val lockFile = LockFile.readToResult().getOrNull() ?: modpackModel.toLockFile()

        // Configuration
        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectProvider = lockFile.getProjectProvider().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        // --

        val importedProjects = modpackModel.toSetOfProjects(lockFile, platforms)

        importedProjects.map { projectIn ->
            launch {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        if (error !is AlreadyAdded) terminal.pError(error)
                    },
                    onRetry = { platform, _ ->
                        promptForProject(platform, terminal, lockFile)
                    },
                    onSuccess = { project, _, reqHandlers ->
                        lockFile.add(project)
                        if (depsFlag) project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        terminal.pSuccess("${project.getFlavoredSlug()} added")
                        Modrinth.checkRateLimit()
                    },
                    lockFile, platforms
                )
            }
        }.joinAll()

        lockFile.write()
    }
}