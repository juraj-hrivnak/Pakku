package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.projects.containsNotProject
import java.io.File

class Diff : CliktCommand("Diff projects in modpack")
{
    private val oldPathArg by argument("path")
    private val newPathArg by argument("path")
    private val markdownDiffOpt by option("--markdown-diff", metavar = "<path>")
    private val markdownOpt by option("--markdown", metavar = "<path>")

    override fun run(): Unit = runBlocking {
        val oldPakkuLock = PakkuLock.readToResultFrom(oldPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val newPakkuLock = PakkuLock.readToResultFrom(newPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val added = newPakkuLock.getAllProjects()
            .mapNotNull { newProject ->
                if (oldPakkuLock.getAllProjects() containsNotProject newProject)
                {
                    newProject
                }
                else null
            }
            .mapNotNull { it.name.values.firstOrNull() }

        added.forEach { terminal.success("+ $it") }

        val removed = oldPakkuLock.getAllProjects()
            .mapNotNull { oldProject ->
                if (newPakkuLock.getAllProjects() containsNotProject oldProject)
                {
                    oldProject
                }
                else null
            }
            .mapNotNull { it.name.values.firstOrNull() }

        removed.forEach { terminal.danger("- $it") }

        if (markdownDiffOpt != null)
        {
            val file = File(markdownDiffOpt)

            file.createNewFile()
            file.outputStream().close()
            file.appendText("```diff\n")
            added.forEach { file.appendText("+ $it\n") }
            removed.forEach { file.appendText("- $it\n") }
            file.appendText("```\n")
        }

        if (markdownOpt != null)
        {
            val file = File(markdownOpt)

            file.createNewFile()
            file.outputStream().close()
            if (added.isNotEmpty())
            {
                file.appendText("### Added\n\n")
                added.forEach { file.appendText("- $it\n") }
                if (removed.isNotEmpty()) file.appendText("\n")
            }
            if (removed.isNotEmpty())
            {
                file.appendText("### Removed\n\n")
                removed.forEach { file.appendText("- $it\n") }
            }
        }

        echo()
    }
}