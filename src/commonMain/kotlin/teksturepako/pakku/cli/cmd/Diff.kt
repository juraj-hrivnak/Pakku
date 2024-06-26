package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.containsNotProject
import java.io.File

class Diff : CliktCommand("Diff projects in modpack")
{
    private val oldPathArg by argument("old-lock-file")
    private val newPathArg by argument("current-lock-file")
    private val markdownDiffOpt by option(
        "--markdown-diff", metavar = "<path>", help = "Export a `.md` file formatted as a diff code block"
    )
    private val markdownOpt by option(
        "--markdown", metavar = "<path>", help = "Export a `.md` file formatted as regular markdown"
    )
    private val verboseOpt: Boolean by option(
        "-v", "--verbose", help = "Gives detailed information on which mods were updated"
    ).flag()

    override fun run(): Unit = runBlocking {
        val oldLockFile = LockFile.readToResultFrom(oldPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        val allOldProjects = oldLockFile.getAllProjects()

        val newLockFile = LockFile.readToResultFrom(newPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        val allNewProjects = newLockFile.getAllProjects()

        val added = allNewProjects.mapNotNull { newProject ->
            if (allOldProjects containsNotProject newProject)
            {
                newProject
            }
            else null
        }.mapNotNull { it.name.values.firstOrNull() }
        added.forEach { terminal.success("+ $it") }

        val removed = allOldProjects.mapNotNull { oldProject ->
            if (allNewProjects containsNotProject oldProject)
            {
                oldProject
            }
            else null
        }.mapNotNull { it.name.values.firstOrNull() }
        removed.forEach { terminal.danger("- $it") }

        val updated: MutableList<String> = mutableListOf()
        val verboseUpdatedFiles: MutableMap<String, String> = mutableMapOf()

        for (oldProject in allOldProjects)
        {
            /** We only care about projects, which previously also existed **/
            if (allNewProjects containsNotProject oldProject) continue

            for (newProject in allNewProjects)
            {
                /** Everything after this is the same project **/
                if (!newProject.isAlmostTheSameAs(oldProject)) continue

                val oldProjectFiles = oldProject.files
                val newProjectFiles = newProject.files

                /** If the project files are not identical, means that files have changed **/
                if (oldProjectFiles == newProjectFiles) continue

                /** On multiloader modpacks, the mod might have been added for another loader, causing the
                 * files key to have changed without an actual update.
                 **/
                if (oldProjectFiles.firstOrNull()?.hashes?.get("sha1") == newProjectFiles.firstOrNull()?.hashes?.get("sha1")) continue
                if (verboseOpt)
                {
                    verboseUpdatedFiles["${oldProjectFiles.firstOrNull()?.fileName}"] =
                        "${newProjectFiles.firstOrNull()?.fileName}"
                }
                else
                {
                    updated.add("${oldProject.name.values.firstOrNull()}")
                }
            }
        }

        if (verboseOpt)
        {
            val maxOldFileLength = verboseUpdatedFiles.keys.maxOfOrNull { it.length } ?: 0
            for ((oldFileName, newFileName) in verboseUpdatedFiles)
            {
                updated.add("${oldFileName.padEnd(maxOldFileLength)} -> $newFileName")
            }
        }
        updated.forEach { terminal.info("! $it") }

        if (markdownDiffOpt != null)
        {
            val file = File(markdownDiffOpt)

            file.createNewFile()
            file.outputStream().close()
            file.appendText("```diff\n")
            added.forEach { file.appendText("+ $it\n") }
            removed.forEach { file.appendText("- $it\n") }
            updated.forEach { file.appendText("! $it\n") }
            file.appendText("```\n")
        }

        if (markdownOpt != null)
        {
            val file = File(markdownOpt)

            file.createNewFile()
            file.outputStream().close()
            if (added.isNotEmpty())
            {
                file.appendText("### Added\n")
                added.forEach { file.appendText("- $it\n") }
                if (removed.isNotEmpty()) file.appendText("\n")
            }
            if (removed.isNotEmpty())
            {
                file.appendText("### Removed\n")
                removed.forEach { file.appendText("- $it\n") }
                if (updated.isNotEmpty()) file.appendText("\n")
            }
            if (updated.isNotEmpty())
            {
                file.appendText("### Updated\n")
                updated.forEach { file.appendText("- $it\n") }
            }
        }
        echo()
    }
}