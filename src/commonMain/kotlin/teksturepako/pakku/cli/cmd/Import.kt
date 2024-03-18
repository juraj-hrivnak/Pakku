package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.Error
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.import
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.promptForProject
import teksturepako.pakku.cli.resolveDependencies

class Import : CliktCommand("Import modpack")
{
    private val pathArg: String by argument("path")

    override fun run() = runBlocking {
        val lockFile = LockFile.readOrNew()

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

        val importedProjects = import(
            onError = { error -> terminal.danger(error.message) },
            pathArg, lockFile, platforms
        )

        importedProjects.map { projectIn ->
            launch {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        if (error !is Error.AlreadyAdded) terminal.danger(error.message)
                    },
                    onRetry = { platform -> promptForProject(platform, terminal, lockFile) },
                    onSuccess = { project, _, reqHandlers ->
                        lockFile.add(project)
                        project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        terminal.success("${project.slug} added")
                        Modrinth.checkRateLimit()
                    },
                    lockFile, platforms
                )
            }
        }.joinAll()

        lockFile.write()
    }
}