package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.RequestCtx
import teksturepako.pakku.api.actions.createRequest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

suspend fun Project.resolveDependencies(
    terminal: Terminal,
    ctx: RequestCtx,
)
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
            debug { terminal.info(dependencyIn.toPrettyString()) }
            dependencyIn.createRequest(
                onError = ctx.onError,
                onRetry = ctx.onRetry,
                onSuccess = { dependency, _, ctx2 ->
                    runBlocking {
                        PakkuLock.addProject(dependency)
                        PakkuLock.addPakkuLink(dependency.pakkuId!!, this@resolveDependencies)

                        dependency.resolveDependencies(terminal, ctx2)
                        terminal.info("${dependency.slug} added")
                    }
                }
            )
        }
    }
}