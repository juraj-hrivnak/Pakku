package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.containsNotProject
import java.io.File

class Diff : CliktCommand("Diff projects in modpack")
{
    private val oldPathArg by argument("path")
    private val newPathArg by argument("path")
    private val markdownDiffOpt by option("--markdown-diff", metavar = "<path>", help = "Export a `.md` file formatted as a diff code block")
    private val markdownOpt by option("--markdown", metavar = "<path>", help = "Export a `.md` file formatted as regular markdown")

    override fun run(): Unit = runBlocking {
        val oldLockFile = LockFile.readToResultFrom(oldPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val newLockFile = LockFile.readToResultFrom(newPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val added = newLockFile.getAllProjects()
            .mapNotNull { newProject ->
                if (oldLockFile.getAllProjects() containsNotProject newProject)
                {
                    newProject
                }
                else null
            }
            .mapNotNull { it.name.values.firstOrNull() }

        added.forEach { terminal.success("+ $it") }

        val removed = oldLockFile.getAllProjects()
            .mapNotNull { oldProject ->
                if (newLockFile.getAllProjects() containsNotProject oldProject)
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