package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.containsNotProject
import teksturepako.pakku.api.projects.containsProject
import java.io.File

class Diff : CliktCommand("Diff projects in modpack")
{
    private val oldPathArg by argument("path")
    private val newPathArg by argument("path")
    private val markdownDiffOpt by option("--markdown-diff", metavar = "<path>", help = "Export a `.md` file formatted as a diff code block")
    private val markdownOpt by option("--markdown", metavar = "<path>", help = "Export a `.md` file formatted as regular markdown")
    private val detailedUpdate: Boolean by option("--detailed-update", help = "Gives detailed information on which mods were updated").flag()

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
            } else null
        }.mapNotNull { it.name.values.firstOrNull() }
        added.forEach { terminal.success("+ $it") }

        val removed = allOldProjects.mapNotNull { oldProject ->
            if (allNewProjects containsNotProject oldProject)
            {
                oldProject
            } else null
        }.mapNotNull { it.name.values.firstOrNull() }
        removed.forEach { terminal.danger("- $it") }

        val updated: MutableList<String> = mutableListOf()
        val oldFiles: MutableList<String> = mutableListOf()
        val newFiles: MutableList<String> = mutableListOf()
        for (oldProject in allOldProjects)
        {
            /** We only care about projects, which previously also existed **/
            if (allNewProjects containsProject oldProject)
            {
                for (newProject in allNewProjects)
                {
                    /** Everything inside if-block is the same project **/
                    if (newProject isAlmostTheSameAs oldProject)
                    {
                        val oldProjectFiles = oldProject.files
                        val newProjectFiles = newProject.files
                        /** If the project files are not identical, means that files have changed **/
                        if (oldProjectFiles != newProjectFiles)
                        {
                            /** On multiloader modpacks, the mod might have been added for another loader, causing the
                             * files key to have changed without an actual update
                             **/
                            if (oldProjectFiles.firstOrNull()?.hashes?.get("sha1") != newProjectFiles.firstOrNull()?.hashes?.get("sha1")                            )
                            {
                                if (detailedUpdate)
                                {
                                    oldFiles.add("${oldProjectFiles.firstOrNull()?.fileName}")
                                    newFiles.add("${newProjectFiles.firstOrNull()?.fileName}")
                                } else
                                {
                                    updated.add("${oldProject.name.values.firstOrNull()}")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (detailedUpdate)
        {
            val maxOldFileLength = oldFiles.maxOfOrNull { it.length } ?: 0
            for (i in oldFiles.indices)
            {
                val oldFileName = oldFiles[i]
                val newFileName = newFiles[i]
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