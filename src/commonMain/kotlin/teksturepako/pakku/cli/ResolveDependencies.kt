package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.info
import teksturepako.pakku.api.actions.RequestHandlers
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.cli.ui.pInfo
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

suspend fun Project.resolveDependencies(
    terminal: Terminal,
    reqHandlers: RequestHandlers,
    lockFile: LockFile,
    configFile: ConfigFile,
    projectProvider: Provider,
    platforms: List<Platform>,
    addAsProjects: Boolean = true
)
{
    val dependencies = this.requestDependencies(projectProvider, lockFile)
    if (dependencies.isEmpty()) return

    terminal.pInfo("Resolving dependencies...")

    for (dependencyIn in dependencies)
    {
        val alias = configFile.projectAliases.keys.find { it in dependencyIn }

        if (lockFile.isProjectAdded(dependencyIn) || alias != null)
        {
            // Link project to dependency if the dependency is already added
            val project = if (alias != null) lockFile.getProject(alias) else lockFile.getProject(dependencyIn)
            project?.pakkuId?.let { pakkuId ->
                lockFile.addPakkuLink(pakkuId, this)
            }
        }
        else if (addAsProjects)
        {
            // Add dependency as a regular project and resolve dependencies for it too
            debug { terminal.info(dependencyIn.toPrettyString()) }
            dependencyIn.createAdditionRequest(
                onError = reqHandlers.onError,
                onSuccess = { dependency, _, depReqHandlers ->
                    // Add dependency
                    lockFile.add(dependency)

                    // Link dependency to parent project
                    lockFile.addPakkuLink(dependency.pakkuId!!, this@resolveDependencies)

                    // Resolve dependencies for dependency
                    dependency.resolveDependencies(terminal, depReqHandlers, lockFile, configFile, projectProvider, platforms)
                    terminal.pInfo("${dependency.getFlavoredSlug()} added")
                },
                lockFile, platforms
            )
        }
    }
}