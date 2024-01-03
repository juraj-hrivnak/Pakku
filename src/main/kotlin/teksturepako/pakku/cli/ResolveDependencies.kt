package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.RequestHandlers
import teksturepako.pakku.api.actions.createRequest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

suspend fun Project.resolveDependencies(
    terminal: Terminal,
    reqHandlers: RequestHandlers,
    pakkuLock: PakkuLock
)
{
    val dependencies = this.requestDependencies(Multiplatform, pakkuLock)
    if (dependencies.isEmpty()) return

    terminal.info("Resolving dependencies...")

    for (dependencyIn in dependencies)
    {
        // Link project to dependency if dependency is already added
        if (pakkuLock.isProjectAdded(dependencyIn))
        {
            pakkuLock.getProject(dependencyIn)?.pakkuId?.let { pakkuId ->
                pakkuLock.addPakkuLink(pakkuId, this)
            }
        } else // Add dependency as a regular project and resolve dependencies for it too
        {
            debug { terminal.info(dependencyIn.toPrettyString()) }
            dependencyIn.createRequest(
                onError = reqHandlers.onError,
                onRetry = reqHandlers.onRetry,
                onSuccess = { dependency, _, depReqHandlers ->
                    runBlocking {
                        pakkuLock.add(dependency)
                        pakkuLock.addPakkuLink(dependency.pakkuId!!, this@resolveDependencies)

                        dependency.resolveDependencies(terminal, depReqHandlers, pakkuLock)
                        terminal.info("${dependency.slug} added")
                    }
                },
                pakkuLock
            )
        }
    }
}