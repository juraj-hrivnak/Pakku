package teksturepako.pakku.api.actions

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.CfFile
import teksturepako.pakku.api.projects.MrFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

object ProjectActions
{
    suspend fun promptForProject(terminal: Terminal, platform: Platform): Project?
    {
        val prompt = StringPrompt("Specify ${platform.name}", terminal).ask()

        if (prompt.isNullOrBlank()) return null

        return Multiplatform.requestProjectWithFiles(
            PakkuLock.getMcVersions(), PakkuLock.getLoaders(), prompt
        )
    }

    suspend fun resolveDependencies(terminal: Terminal, project: Project)
    {
        terminal.info("Resolving dependencies...")

        project.files
            .filterIsInstance<CfFile>()
            .flatMap { it.requiredDependencies ?: emptyList() }
            .mapNotNull {
                Multiplatform.requestProjectWithFiles(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), it)
            }
            .debug { terminal.info(it.toPrettyString()) }
            .forEach {
                PakkuLock.addProject(it).also { added ->
                    if (added == true)
                    {
                        project.pakkuLinks.add(it.pakkuId!!)
                        terminal.info("${it.slug} added")
                    } else if (added == null)
                    {
                        PakkuLock.getProject(it.slug.values.first())?.let { project ->
                            project.pakkuLinks.add(project.pakkuId!!)
                        }
                    }
                }
            }

        debug { terminal.println() }

        project.files
            .filterIsInstance<MrFile>()
            .flatMap { it.requiredDependencies ?: emptyList() }
            .mapNotNull {
                Multiplatform.requestProjectWithFiles(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), it)
            }
            .debug { terminal.info(it.toPrettyString()) }
            .forEach {
                PakkuLock.addProject(it).also { added ->
                    if (added == true)
                    {
                        project.pakkuLinks.add(it.pakkuId!!)
                        terminal.info("${it.slug} added")
                    } else if (added == null)
                    {
                        PakkuLock.getProject(it.slug.values.first())?.let { project ->
                            project.pakkuLinks.add(project.pakkuId!!)
                        }
                    }
                }
            }

        terminal.println()
    }
}