package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.containsNotProject
import java.io.File
import kotlin.collections.Set

class Diff : CliktCommand()
{
    override fun help(context: Context) = "Diff projects in modpack"

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
        val allOldMCVersions = oldLockFile.getMcVersions().toSet()

        val newLockFile = LockFile.readToResultFrom(newPathArg).getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }
        val allNewProjects = newLockFile.getAllProjects()
        val allNewMCVersions = newLockFile.getMcVersions().toSet()

        val addedMCVersions = allNewMCVersions - allOldMCVersions
        val removedMCVersions = allOldMCVersions - allNewMCVersions

        addedMCVersions.forEach { terminal.success("+ $it") }
        removedMCVersions.forEach { terminal.danger("- $it") }

        val allOldModLoadersAndVersions = oldLockFile.getLoadersWithVersions().toSet()
        val allNewModLoadersAndVersions = newLockFile.getLoadersWithVersions().toSet()

        // Only compute difference if the modloaders actually changed
        val didModLoaderUpdate = allOldModLoadersAndVersions != allNewModLoadersAndVersions
        var addedModLoaders: Set<String>
        var removedModLoaders: Set<String>
        var updatedModLoaders: List<String>
        if (didModLoaderUpdate)
        {
            // Extract modloaders (first element of the pair) for both old and new versions
            val oldModLoaders = allOldModLoadersAndVersions.map { it.first }.toSet()
            val newModLoaders = allNewModLoadersAndVersions.map { it.first }.toSet()

            // Added modloaders: Present in new but not in old
            addedModLoaders = newModLoaders - oldModLoaders
            // .capitalize() is deprecated ...
            addedModLoaders = addedModLoaders.map { loader -> loader.replaceFirstChar { it.titlecase() } }.toSet()
            addedModLoaders.forEach { terminal.success("+ $it") }

            // Removed modloaders: Present in old but not in new
            removedModLoaders = oldModLoaders - newModLoaders
            removedModLoaders = removedModLoaders.map { loader -> loader.replaceFirstChar { it.titlecase() } }.toSet()
            removedModLoaders.forEach { terminal.danger("- $it") }

            if (verboseOpt) {
                // Find the max length of the loader name and old version to align arrows
                val maxLength = allOldModLoadersAndVersions.maxOfOrNull { oldModLoader ->
                    "${oldModLoader.first.replaceFirstChar { it.uppercase() }} ${oldModLoader.second}".length
                } ?: 0

                // Updated modloaders: Same modloader in both sets but with different versions
                updatedModLoaders = allNewModLoadersAndVersions.filter { newModLoader ->
                    allOldModLoadersAndVersions.any { oldModLoader ->
                        oldModLoader.first == newModLoader.first && oldModLoader.second != newModLoader.second
                    }
                }.map { newModLoader ->
                    val oldModLoader = allOldModLoadersAndVersions.first { it.first == newModLoader.first }
                    val oldModLoaderStr = "${oldModLoader.first.replaceFirstChar { it.uppercase() }} ${oldModLoader.second}"
                    val newModLoaderStr = "${newModLoader.first.replaceFirstChar { it.uppercase() }} ${newModLoader.second}"

                    // Pad the oldModLoaderStr to match maxLength, then add the arrow and the new version
                    oldModLoaderStr.padEnd(maxLength) + " -> " + newModLoaderStr
                }
            } else {
                // Updated modloaders: Same modloader in both sets but with different versions
                updatedModLoaders = allNewModLoadersAndVersions.filter { newModLoader ->
                    allOldModLoadersAndVersions.any { oldModLoader ->
                        oldModLoader.first == newModLoader.first && oldModLoader.second != newModLoader.second
                    }
                }.map { loader -> loader.first.replaceFirstChar { it.uppercase() } }
            }
            updatedModLoaders.forEach { terminal.info("! $it") }
        }
        else
        {
            addedModLoaders = emptySet()
            removedModLoaders = emptySet()
            updatedModLoaders = emptyList()
        }

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
                val oldFileHash = oldProjectFiles.firstOrNull()?.hashes?.get("sha1")
                val newFileHash = newProjectFiles.firstOrNull()?.hashes?.get("sha1")
                if (oldFileHash == newFileHash) continue

                if (verboseOpt)
                {
                    val oldFileName = oldProjectFiles.firstOrNull()?.fileName
                    val newFileName = newProjectFiles.firstOrNull()?.fileName
                    val fileNamesNotNullOrEmpty = !oldFileName.isNullOrEmpty() && !newFileName.isNullOrEmpty()

                    if (fileNamesNotNullOrEmpty) verboseUpdatedFiles["$oldFileName"] = "$newFileName"
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
            addedMCVersions.forEach { file.appendText("+ $it\n") }
            removedMCVersions.forEach { file.appendText("- $it\n") }
            if (addedMCVersions.isNotEmpty() || removedMCVersions.isNotEmpty()) file.appendText("\n")
            addedModLoaders.forEach { file.appendText("+ $it\n") }
            removedModLoaders.forEach { file.appendText("- $it\n") }
            updatedModLoaders.forEach { file.appendText("! $it\n") }
            if (addedModLoaders.isNotEmpty() || removedModLoaders.isNotEmpty() || updatedModLoaders.isNotEmpty()) file.appendText(
                "\n"
            )
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
            if (addedMCVersions.isNotEmpty() || removedMCVersions.isNotEmpty()) {
                file.appendText("## Minecraft\n\n")

                if (addedMCVersions.isNotEmpty()) {
                    file.appendText("### Added\n\n")
                    addedMCVersions.forEach {file.appendText("- $it\n")}
                    if (addedModLoaders.isNotEmpty() || removedModLoaders.isNotEmpty() || updatedModLoaders.isNotEmpty() || removedMCVersions.isNotEmpty()) file.appendText("\n")
                }

                if (removedMCVersions.isNotEmpty()) {
                    file.appendText("### Removed\n\n")
                    removedMCVersions.forEach {file.appendText("- $it\n")}
                    if (addedModLoaders.isNotEmpty() || removedModLoaders.isNotEmpty() || updatedModLoaders.isNotEmpty()) file.appendText("\n")
                }
            }

            if (addedModLoaders.isNotEmpty() || removedModLoaders.isNotEmpty() || updatedModLoaders.isNotEmpty())
            {
                file.appendText("## Loaders\n\n")

                if (addedModLoaders.isNotEmpty())
                {
                    file.appendText("### Added\n\n")
                    addedModLoaders.forEach { file.appendText("- $it\n") }
                    if (removed.isNotEmpty() || updated.isNotEmpty()) file.appendText("\n")
                }

                if (removedModLoaders.isNotEmpty())
                {
                    file.appendText("### Removed\n\n")
                    removedModLoaders.forEach { file.appendText("- $it\n") }
                    if (updated.isNotEmpty()) file.appendText("\n")
                }

                if (updatedModLoaders.isNotEmpty())
                {
                    file.appendText("### Updated\n\n")
                    updatedModLoaders.forEach { file.appendText("- $it\n") }
                }
                if (updatedModLoaders.isNotEmpty() || removed.isNotEmpty() || updated.isNotEmpty()) file.appendText("\n")
            }

            if (added.isNotEmpty() || removed.isNotEmpty() || updated.isNotEmpty())
            {
                file.appendText("## Projects\n\n")

                if (added.isNotEmpty())
                {
                    file.appendText("### Added\n\n")
                    added.forEach { file.appendText("- $it\n") }
                    if (removed.isNotEmpty() || updated.isNotEmpty()) file.appendText("\n")
                }
                if (removed.isNotEmpty())
                {
                    file.appendText("### Removed\n\n")
                    removed.forEach { file.appendText("- $it\n") }
                    if (updated.isNotEmpty()) file.appendText("\n")
                }
                if (updated.isNotEmpty())
                {
                    file.appendText("### Updated\n\n")
                    updated.forEach { file.appendText("- $it\n") }
                }
            }
        }
        echo()
    }
}