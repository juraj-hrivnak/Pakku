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
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies

class Import : CliktCommand("Import modpack")
{
    private val path: String by argument("path")

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        val cfManifest = importCfManifest(path)

        if (cfManifest == null)
        {
            terminal.danger("Could not import from $path")
            return@runBlocking
        }

        val projects = async { CurseForge.requestMultipleProjects(cfManifest.files.map { it.projectID }) }
        val files = async {
            CurseForge.requestMultipleProjectFiles(
                pakkuLock.getMcVersions(), pakkuLock.getLoaders(), cfManifest.files.map { it.fileID }
            )
        }

        projects.await().forEach { project ->
            files.await().forEach { file ->
                project.apply { if (id[CurseForge.serialName] == file.parentId) this.files.add(file)}
            }
        }

        projects.await().forEach { projectIn ->
            projectIn.createAdditionRequest(
                onError = { error ->
                    if (error !is Error.CouldNotAdd) terminal.danger(error)
                },
                onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                onSuccess = { project, _, reqHandlers ->
                    runBlocking {
                        pakkuLock.add(project)
                        project.resolveDependencies(
                            terminal = terminal,
                            reqHandlers = reqHandlers,
                            pakkuLock = pakkuLock,
                            projectProvider = CurseForge,
                            platforms = listOf(CurseForge)
                        )
                        terminal.success("${project.slug} added")
                    }
                },
                pakkuLock = pakkuLock,
                platforms = listOf(CurseForge)
            )
            terminal.println()
        }

        pakkuLock.write()
    }
}