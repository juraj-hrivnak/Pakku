package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRequest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.Project

suspend fun Project.resolveDependencies(terminal: Terminal)
{
    val dependencies = this.requestDependencies(Multiplatform)
    if (dependencies.isEmpty()) return

    terminal.info("Resolving dependencies...")

    for (dependencyIn in dependencies)
    {
        // Link project to dependency if dependency is already added
        if (PakkuLock.isProjectAdded(dependencyIn))
        {
            PakkuLock.getProject(dependencyIn)?.pakkuId?.let { pakkuId ->
                PakkuLock.addPakkuLink(pakkuId, this)
            }
        } else
        {
            dependencyIn.createRequest(
                onError = { error -> terminal.danger(error) },
                onRetry = { platform -> promptForProject(platform, terminal) },
                onSuccess = { dependency, _ ->
                    runBlocking {
                        PakkuLock.addProject(dependency)
                        PakkuLock.addPakkuLink(dependency.pakkuId!!, this@resolveDependencies)

                        dependency.resolveDependencies(terminal)
                        terminal.info("${dependency.slug} added")
                    }
                }
            )
        }
    }
}