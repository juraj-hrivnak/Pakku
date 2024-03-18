package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.promptForProject
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.ynPrompt

class Add : CliktCommand("Add projects")
{
    private val projectArgs: List<String> by argument("projects").multiple(required = true)

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

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

        for (projectIn in projectArgs.map { arg ->
            projectProvider.requestProjectWithFiles(lockFile.getMcVersions(), lockFile.getLoaders(), arg)
        })
        {
            projectIn.createAdditionRequest(
                onError = { error -> terminal.danger(error.message) },
                onRetry = { platform -> promptForProject(platform, terminal, lockFile) },
                onSuccess = { project, isRecommended, reqHandlers ->
                    if (ynPrompt("Do you want to add ${project.slug}?", terminal, isRecommended))
                    {
                        lockFile.add(project)
                        lockFile.linkProjectToDependents(project)
                        project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        terminal.success("${project.slug} added")
                    }
                },
                lockFile, platforms
            )

            echo()
        }

        lockFile.write()
    }
}