package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.ProjectActions
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.debug
import teksturepako.pakku.toPrettyString

class Add : CliktCommand("Add mods")
{
    private val mods: List<String> by argument().multiple(required = true)

    override fun run() = runBlocking {
        for (deferred in mods.map { arg ->
            async {
                Multiplatform.requestProjectFiles(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), arg) to arg
            }
        })
        {
            var (project, arg) = deferred.await()
            var defaultPrompt = true

            if (project == null)
            {
                terminal.warning("$arg not found")
                echo()
                continue
            }

            if (PakkuLock.isProjectAdded(project))
            {
                terminal.danger("Could not add ${project.slug}. It is already added")
                echo()
                continue
            }

            for (platform in Multiplatform.platforms)
            {
                if (project.isNotOnPlatform(platform))
                {
                    terminal.danger("$arg was not found on ${platform.name}")
                    val project2 = ProjectActions.promptForProject(terminal, platform)

                    if (project2 != null && project2.hasFilesForPlatform(platform))
                    {
                        project += project2
                        terminal.success("$arg was found on ${platform.name}")
                    }
                    echo()
                }

                if (project.hasNoFilesForPlatform(platform))
                {
                    defaultPrompt = false
                    terminal.danger("No files for $arg found on ${platform.name}")
                    echo()
                }
            }

            if (project.hasNoFiles())
            {
                terminal.danger("No files found for $arg")
                echo()
                continue
            }

            if (project.fileNamesNotMatchAcrossPlatforms(Multiplatform.platforms))
            {
                defaultPrompt = false
                terminal.danger("$arg versions do not match across platforms")
                terminal.danger(json.encodeToString(project.files.map { it.fileName }))
                echo()
            }

            if (YesNoPrompt("Do you want to add ${project.slug}?", terminal, defaultPrompt).ask() == true)
            {
                echo()
                PakkuLock.addProject(project)
                terminal.success("${project.slug} added")
            }

            debug {
                echo()
                terminal.success(project.toPrettyString())
            }

            echo()
        }
        echo(Multiplatform.platforms.map { it.name })
    }

}