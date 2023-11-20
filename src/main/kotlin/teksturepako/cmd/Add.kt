package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.YesNoPrompt
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import teksturepako.data.PakkuLock
import teksturepako.data.allEmpty
import teksturepako.data.json
import teksturepako.debug
import teksturepako.platforms.Multiplatform
import teksturepako.toPrettyString

class Add : CliktCommand("Add mods")
{
    private val mods: List<String> by argument().multiple(required = true)

    override fun run() = runBlocking {
        mods.map { arg ->
            async {
                Multiplatform.requestProjectFile(PakkuLock.getMcVersion(), PakkuLock.getModLoader(), arg) to arg
            }
        }.forEach {
            val (result, arg) = it.await()

            if (result != null)
            {
                var defaultPrompt = true

                if (result.files.values.all { value -> value.allEmpty() })
                {
                    terminal.danger("No versions found for $arg")
                    terminal.danger(json.encodeToString(result.files))
                    echo()
                    defaultPrompt = false
                }

                if (result.files.values.any { list -> list.any { projectFile -> projectFile.fileName != list.first().fileName } })
                {
                    terminal.danger("$arg versions do not match across platforms")
                    terminal.danger(json.encodeToString(result.files))
                    echo()
                    defaultPrompt = false
                }

                if (YesNoPrompt("Do you want to add ${result.slug}?", terminal, defaultPrompt).ask() == true)
                {
                    echo()
                    terminal.success("${result.slug} added")
                    PakkuLock.addProject(result)
                }

                debug {
                    echo()
                    terminal.success(result.toPrettyString())
                }
            } else
            {
                terminal.warning("$arg not found")
            }
            echo()
        }
        echo(Multiplatform.platforms.map { it.name })
    }

}