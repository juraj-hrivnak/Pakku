package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.*
import okio.Path.Companion.toPath
import teksturepako.pakku.api.actions.Error
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.importCfManifest
import teksturepako.pakku.api.actions.importMrPack
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.debug

class Import : CliktCommand("Import modpack")
{
    private val pathArg: String by argument("path")
    // TODO: Remove these flags
    private val cfFlag: Boolean by option("-cf", help = "Import CurseForge").flag()
    private val mrFlag: Boolean by option("-mr", help = "Import Modrinth").flag()

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        // Configuration
        val platforms: List<Platform> = pakkuLock.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectProvider = pakkuLock.getProjectProvider().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        // --

        val processedProjects = if (cfFlag) // CurseForge import
        {
            val cfManifest = importCfManifest(pathArg)

            if (cfManifest == null)
            {
                terminal.danger("Could not import from $pathArg")
                return@runBlocking
            }

            val projects = CurseForge.requestMultipleProjects(cfManifest.files.map { it.projectID })
            val files = CurseForge.requestMultipleProjectFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), cfManifest.files.map { it.fileID })

            projects.assignFiles(files, CurseForge)

            // Modrinth
            if (Modrinth in platforms)
            {
                debug { terminal.warning("Modrinth sub-import") }

                val slugs = projects.mapNotNull { project ->
                    project.slug[CurseForge.serialName]
                }

                val mrProjects = Modrinth.requestMultipleProjectsWithFiles(
                    pakkuLock.getMcVersions(), pakkuLock.getLoaders(), slugs, 1
                )

                projects.combineWith(mrProjects)
            }
            else projects
        }
        else if (mrFlag) // Modrinth import
        {
            val mrManifest = importMrPack(pathArg.toPath())

            if (mrManifest == null)
            {
                terminal.danger("Could not import from $pathArg")
                return@runBlocking
            }

            val projects = Modrinth.requestMultipleProjectsWithFilesFromHashes(
                mrManifest.files.map { it.hashes.sha1 }, "sha1"
            )

            // CurseForge
            if (CurseForge in platforms)
            {
                debug { terminal.warning("CurseForge sub-import") }

                val slugs = projects.mapNotNull { project ->
                    project.slug[Modrinth.serialName]
                }

                val cfProjects = slugs.map { slug ->
                    async {
                        CurseForge.requestProjectFromSlug(slug)?.apply {
                            files += CurseForge.requestFilesForProject(
                                pakkuLock.getMcVersions(), pakkuLock.getLoaders(), this
                            )
                        }
                    }
                }.awaitAll().filterNotNull()

                projects.combineWith(cfProjects).debug { terminal.warning(it.map { p -> p.slug }) }
            }
            else projects
        }
        else setOf<Project>()

        processedProjects.map { projectIn ->
            launch {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        if (error !is Error.CouldNotAdd) terminal.danger(error.message)
                    },
                    onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                    onSuccess = { project, _, reqHandlers ->
                        pakkuLock.add(project)
                        project.resolveDependencies(terminal, reqHandlers, pakkuLock, projectProvider, platforms)
                        terminal.success("${project.slug} added")
                        Modrinth.checkRateLimit()
                    },
                    pakkuLock,
                    platforms
                )
            }
        }.joinAll()

        pakkuLock.write()
    }
}