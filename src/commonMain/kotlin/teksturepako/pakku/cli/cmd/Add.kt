package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.arg.splitProjectArg
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.*

class Add : CliktCommand("Add projects")
{
    private val projectArgs: List<String> by argument("projects").multiple(required = true)
    private val noDepsFlag: Boolean by option("-D", "--no-deps", help = "Ignore resolving dependencies").flag()

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

        for ((projectIn, arg) in projectArgs.map { arg ->
            val (input, fileId) = splitProjectArg(arg)

            projectProvider.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), input, fileId
            ) to arg
        })
        {
            projectIn.createAdditionRequest(
                onError = { error -> terminal.pError(error, arg) },
                onRetry = { platform, project ->
                    val fileId = project.getFilesForPlatform(platform).firstOrNull()?.id

                    promptForProject(platform, terminal, lockFile, fileId)
                },
                onSuccess = { project, isRecommended, reqHandlers ->
                    val slugMsg = project.getFlavoredSlug()

                    if (ynPrompt("Do you want to add $slugMsg?", terminal, isRecommended))
                    {
                        lockFile.add(project)
                        lockFile.linkProjectToDependents(project)

                        if (!noDepsFlag)
                        {
                            project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        }

                        terminal.pSuccess("$slugMsg added")
                    }
                },
                lockFile, platforms
            )

            echo()
        }

        lockFile.write()
    }
}