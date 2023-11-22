package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import teksturepako.data.PakkuLock
import teksturepako.data.allEmpty
import teksturepako.data.allNotEqual
import teksturepako.data.json
import teksturepako.debug
import teksturepako.platforms.CurseForge
import teksturepako.platforms.Modrinth
import teksturepako.platforms.Multiplatform
import teksturepako.toPrettyString

class Add : CliktCommand("Add mods")
{
    private val mods: List<String> by argument().multiple(required = true)

    override fun run() = runBlocking {
        for (deferred in mods.map { arg ->
            async {
                Multiplatform.requestProjectFile(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), arg) to arg
            }
        }) {
            var (project, arg) = deferred.await()

            if (project != null) {
                var defaultPrompt = true

                if (project.files.allEmpty()) {
                    terminal.danger("No versions found for $arg")
                    echo()
                    defaultPrompt = false
                } else {
                    when {
                        CurseForge.serialName !in project.slug.keys -> {
                            terminal.danger("$arg was not found on ${CurseForge.name}")
                            val prompt = StringPrompt("Specify ${CurseForge.name}", terminal).ask()

                            if (!prompt.isNullOrBlank()) {
                                val result2 = Multiplatform.requestProjectFile(
                                    PakkuLock.getMcVersions(),
                                    PakkuLock.getLoaders(),
                                    prompt
                                )
                                if (result2 != null) if (result2 != project) project += result2
                            }

                            echo()
                        }

                        Modrinth.serialName !in project.slug.keys -> {
                            terminal.danger("$arg was not found on ${Modrinth.name}")
                            val prompt = StringPrompt("Specify ${Modrinth.name}", terminal).ask()

                            if (!prompt.isNullOrBlank()) {
                                val result2 = Multiplatform.requestProjectFile(
                                    PakkuLock.getMcVersions(),
                                    PakkuLock.getLoaders(),
                                    prompt
                                )
                                if (result2 != null) if (result2 != project) project += result2
                            }

                            echo()
                        }
                    }

                    when
                    {
                        CurseForge.serialName !in project.files.map { it.type } ->
                        {
                            terminal.danger("No versions for $arg found on ${CurseForge.name}")
                            echo()
                            defaultPrompt = false
                        }
                        Modrinth.serialName !in project.files.map { it.type } ->
                        {
                            terminal.danger("No versions for $arg found on ${Modrinth.name}")
                            echo()
                            defaultPrompt = false
                        }
                    }

                    if (project.files.asSequence()
                            .map { it.fileName }
                            .chunked(Multiplatform.platforms.size)
                            .any { it.allNotEqual() }
                        ) {
                        terminal.danger("$arg versions do not match across platforms")
                        terminal.danger(json.encodeToString(project.files.map { it.fileName }))
                        echo()
                        defaultPrompt = false
                    }
                }

                if (YesNoPrompt("Do you want to add ${project.slug}?", terminal, defaultPrompt).ask() == true) {
                    echo()
                    PakkuLock.addProject(project)
                    terminal.success("${project.slug} added")
                }

                debug {
                    echo()
                    terminal.success(project.toPrettyString())
                }
            } else {
                terminal.warning("$arg not found")
            }
            echo()
        }
        echo(Multiplatform.platforms.map { it.name })
    }

}