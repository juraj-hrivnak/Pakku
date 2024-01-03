package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createRequest
import teksturepako.pakku.api.actions.importCfManifest
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.promptForProject
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

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

        for (deferredProject in cfManifest.files.map { file ->
            async {
                var p = CurseForge.requestProjectWithFilesFromIds(pakkuLock.getMcVersions(), pakkuLock.getLoaders(),
                    file.projectID, file.fileID)
                if (p != null)
                {
                    val mr = Modrinth.requestProjectWithFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), p.slug[CurseForge.serialName]!!)
                    if (mr != null)
                    {
                        if (p.type == mr.type) p += mr
                    }
                }
                p
            }
        })
        {
            debug { echo(deferredProject.await()?.toPrettyString()) }
            deferredProject.await().createRequest(
                onError = { error -> terminal.danger(error) },
                onRetry = { platform -> promptForProject(platform, terminal, pakkuLock) },
                onSuccess = { project, _, reqHandlers ->
                    launch {
                        pakkuLock.add(project)
                        project.resolveDependencies(terminal, reqHandlers, pakkuLock)
                        terminal.success("${project.slug} added")
                    }
                },
                pakkuLock
            )
            terminal.println()
        }

        pakkuLock.write()
    }
}