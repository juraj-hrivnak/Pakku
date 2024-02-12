package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.containsProject


class Ls : CliktCommand("List projects")
{
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projects = pakkuLock.getAllProjects()
        val platforms: List<Platform> = pakkuLock.getPlatforms().getOrDefault(listOf())

        val updatedProjects = Multiplatform.updateMultipleProjectsWithFiles(
            pakkuLock.getMcVersions(), pakkuLock.getLoaders(), projects.toMutableSet(), numberOfFiles = 1
        )

        for (project in projects)
        {
            val upds = when (project.updateStrategy)
            {
                UpdateStrategy.LATEST ->
                {
                    if (updatedProjects containsProject project)
                    {
                        blue(project.updateStrategy.short)
                    }
                    else brightGreen(project.updateStrategy.short)
                }
                UpdateStrategy.NONE   -> red(project.updateStrategy.short)
                else                  -> cyan(project.updateStrategy.short)
            }

            val name: String? = when
            {
                project.redistributable ->
                {
                    if (project.hasNoFiles()) project.name.values.firstOrNull()?.let { red(it) }
                    else project.name.values.firstOrNull()
                }
                else                    ->
                {
                    if (project.hasNoFiles()) project.name.values.firstOrNull()?.let {
                        TextStyle(bgColor = gray, color = red)("⚠$it")
                    } else project.name.values.firstOrNull()?.let {
                        TextStyle(bgColor = gray, color = black)("⚠$it")
                    }
                }
            }

            val deps: String = when
            {
                project.pakkuLinks.size > 1  -> "${project.pakkuLinks.size} deps"
                project.pakkuLinks.size == 1 -> "1 dep "
                else                         -> "      "
            }

            val targets: String = platforms.joinToString(" ") {
                if (project.hasFilesOnPlatform(it))
                {
                    TextStyle(
                        color = brightGreen,
                        hyperlink = "${it.siteUrl}/${project.slug[it.serialName]}"
                    )(it.shortName)
                }
                else red(it.shortName)
            }

            terminal.println(" $deps | $targets | $upds$name")
        }
        echo()
        terminal.info("Projects total: ${projects.size}")
        echo()
    }
}