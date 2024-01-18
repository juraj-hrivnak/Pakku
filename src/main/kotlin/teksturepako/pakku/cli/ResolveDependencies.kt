package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.RequestHandlers
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

suspend fun Project.resolveDependencies(
    terminal: Terminal,
    reqHandlers: RequestHandlers,
    pakkuLock: PakkuLock,
    projectProvider: IProjectProvider,
    platforms: List<Platform>,
    addAsProjects: Boolean = true
)
{
    val dependencies = this.requestDependencies(projectProvider, pakkuLock)
    if (dependencies.isEmpty()) return

    terminal.info("Resolving dependencies...")

    for (dependencyIn in dependencies)
    {
        if (pakkuLock.isProjectAdded(dependencyIn))
        {
            /** Link project to dependency if dependency is already added */
            pakkuLock.getProject(dependencyIn)?.pakkuId?.let { pakkuId ->
                pakkuLock.addPakkuLink(pakkuId, this)
            }
        } else if (addAsProjects)
        {
            /** Add dependency as a regular project and resolve dependencies for it too */
            debug { terminal.info(dependencyIn.toPrettyString()) }
            dependencyIn.createAdditionRequest(
                onError = reqHandlers.onError,
                onRetry = reqHandlers.onRetry,
                onSuccess = { dependency, _, depReqHandlers ->
                    /** Add dependency */
                    pakkuLock.add(dependency)

                    /** Link dependency to parent project */
                    pakkuLock.addPakkuLink(dependency.pakkuId!!, this@resolveDependencies)

                    /** Resolve dependencies for dependency */
                    dependency.resolveDependencies(terminal, depReqHandlers, pakkuLock, projectProvider, platforms)
                    terminal.info("${dependency.slug} added")
                },
                pakkuLock,
                platforms
            )
        }
    }
}