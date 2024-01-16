package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.Error
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.importCfManifest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies

class Import : CliktCommand("Import modpack")
{
    private val pathArg: String by argument("path")

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        // TODO: Configuration
        val platforms: List<Platform> = Multiplatform.platforms
        val projectProvider = Multiplatform
        // --

        val cfManifest = importCfManifest(pathArg)

        if (cfManifest == null)
        {
            terminal.danger("Could not import from $pathArg")
            return@runBlocking
        }

        val projects = async { CurseForge.requestMultipleProjects(cfManifest.files.map { it.projectID }) }
        val files = async { CurseForge.requestMultipleProjectFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), cfManifest.files.map { it.fileID }) }

        projects.await().map { project ->
            files.await().forEach { file ->
                if (project.id[CurseForge.serialName] == file.parentId) project.files.add(file)
            }

            // Modrinth
            val mrProject = Modrinth.requestProject(project.slug[CurseForge.serialName]!!)

            mrProject?.let {
                Modrinth.requestFilesForProject(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), it, 1)
            }?.let { project.files.addAll(it) }
        }

        projects.await().forEach { projectIn ->
            projectIn.createAdditionRequest(
                onError = { error ->
                    if (error !is Error.CouldNotAdd) terminal.danger(error.message)
                },
                onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                onSuccess = { project, _, reqHandlers ->
                    runBlocking {
                        pakkuLock.add(project)
                        project.resolveDependencies(terminal, reqHandlers, pakkuLock, projectProvider, platforms)
                        terminal.success("${project.slug} added")
                    }
                },
                pakkuLock,
                platforms
            )
        }

        pakkuLock.write()
    }
}