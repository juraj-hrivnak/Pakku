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
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies

class Import : CliktCommand("Import modpack")
{
    private val pathArg: String by argument("path")

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        // Configuration
        val platforms: List<Platform> = pakkuLock.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectProvider = pakkuLock.getProjectProvider().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        // --

        val importedProjects = import(
            onError = { error -> terminal.danger(error.message) },
            pathArg, pakkuLock, platforms
        )

        importedProjects.map { projectIn ->
            launch {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        if (error !is Error.AlreadyAdded) terminal.danger(error.message)
                    },
                    onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                    onSuccess = { project, _, reqHandlers ->
                        pakkuLock.add(project)
                        project.resolveDependencies(terminal, reqHandlers, pakkuLock, projectProvider, platforms)
                        terminal.success("${project.slug} added")
                        Modrinth.checkRateLimit()
                    },
                    pakkuLock, platforms
                )
            }
        }.joinAll()

        pakkuLock.write()
    }
}