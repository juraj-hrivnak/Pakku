package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRequest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies

class Add : CliktCommand("Add projects")
{
    private val projects: List<String> by argument().multiple(required = true)

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        for (projectIn in projects.map { arg ->
            Multiplatform.requestProjectWithFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), arg)
        })
        {
            projectIn.createRequest(
                onError = { error -> terminal.danger(error) },
                onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                onSuccess = { project, isRecommended, reqHandlers ->
                    runBlocking {
                        if (YesNoPrompt("Do you want to add ${project.slug}?", terminal, isRecommended).ask() == true)
                        {
                            pakkuLock.add(project)
                            project.resolveDependencies(terminal, reqHandlers, pakkuLock)
                            terminal.success("${project.slug} added")
                        }
                    }
                },
                pakkuLock
            )
            terminal.println()
        }
        pakkuLock.write()
    }
}