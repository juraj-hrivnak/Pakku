package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.success
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
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
    private val headerSizeOpt by option(
        "-h", "--header-size", help = "Specifies the base header size. Default = 2",
    ).int().restrictTo(0..5).default(2)

    override fun run(): Unit = runBlocking {
        val oldLockFile = LockFile.readToResultFrom(oldPathArg).getOrElse {
            terminal.danger("${it.message}\n")
            return@runBlocking
        }
        val allOldMCVersions = oldLockFile.getMcVersions().toSet()
        val allOldModLoadersAndVersions = oldLockFile.getLoadersWithVersions().toSet()
        val allOldProjects = oldLockFile.getAllProjects()

        val newLockFile = LockFile.readToResultFrom(newPathArg).getOrElse {
            terminal.danger("${it.message}\n")
            return@runBlocking
        }
        val allNewMCVersions = newLockFile.getMcVersions().toSet()
        val allNewModLoadersAndVersions = newLockFile.getLoadersWithVersions().toSet()
        val allNewProjects = newLockFile.getAllProjects()

        // Handle MC Version change
        val didMCVersionsChange = allOldMCVersions != allNewMCVersions
        val addedMCVersions = if (didMCVersionsChange)
        {
            getAddedMCVersionsAndLoaders(allOldMCVersions, allNewMCVersions)
        }
        else emptySet()

        val removedMCVersions = if (didMCVersionsChange)
        {
            getRemovedMCVersionsAndLoaders(allOldMCVersions, allNewMCVersions)
        }
        else emptySet()

        // Handle Mod Loader change
        val didModLoaderChange = allOldModLoadersAndVersions != allNewModLoadersAndVersions
        val addedModLoaders = if (didModLoaderChange)
        {
            getAddedMCVersionsAndLoaders(
                allOldModLoadersAndVersions.map { it.first }.toSet(),
                allNewModLoadersAndVersions.map { it.first }.toSet()
            ).map { loader -> loader.replaceFirstChar { it.titlecase() } }.toSet()
        }
        else emptySet()

        val removedModLoaders = if (didModLoaderChange)
        {
            getRemovedMCVersionsAndLoaders(
                allOldModLoadersAndVersions.map { it.first }.toSet(),
                allNewModLoadersAndVersions.map { it.first }.toSet()
            ).map { loader -> loader.replaceFirstChar { it.titlecase() } }.toSet()
        }
        else emptySet()

        val updatedModLoaders = if (didModLoaderChange)
        {
            getUpdatedModLoaders(
                allOldModLoadersAndVersions, allNewModLoadersAndVersions
            ).map { loader -> loader.replaceFirstChar { it.titlecase() } }.toSet()
        }
        else emptySet()

        // Handle Project change
        val didProjectsChange = allOldProjects != allNewProjects
        val addedProjects = if (didProjectsChange)
        {
            getAddedMCVersionsAndLoaders(
                allOldProjects.mapNotNull { it.name.values.firstOrNull() }.toSet(),
                allNewProjects.mapNotNull { it.name.values.firstOrNull() }.toSet()
            )
        }
        else emptySet()

        val removedProjects = if (didProjectsChange)
        {
            getRemovedMCVersionsAndLoaders(
                allOldProjects.mapNotNull { it.name.values.firstOrNull() }.toSet(),
                allNewProjects.mapNotNull { it.name.values.firstOrNull() }.toSet()
            )
        }
        else emptySet()

        val updatedProjects = if (didProjectsChange) getUpdatedProjects(allOldProjects, allNewProjects) else emptyList()

        printDiffChangesToTerminal(
            addedMCVersions,
            removedMCVersions,
            addedModLoaders,
            removedModLoaders,
            updatedModLoaders,
            addedProjects,
            removedProjects,
            updatedProjects
        )

        writeDiffChangesToFile(
            addedMCVersions,
            removedMCVersions,
            addedModLoaders,
            removedModLoaders,
            updatedModLoaders,
            addedProjects,
            removedProjects,
            updatedProjects,
            didMCVersionsChange,
            didModLoaderChange,
            didProjectsChange
        )
    }

    private fun getAddedMCVersionsAndLoaders(allOldVersions: Set<String>, allNewVersions: Set<String>): Set<String>
    {
        return allNewVersions - allOldVersions
    }

    private fun getRemovedMCVersionsAndLoaders(allOldVersions: Set<String>, allNewVersions: Set<String>): Set<String>
    {
        return allOldVersions - allNewVersions
    }

    private fun getUpdatedModLoaders(
        allOldModLoadersAndVersions: Set<Pair<String, String>>, allNewModLoadersAndVersions: Set<Pair<String, String>>
    ): List<String>
    {
        val updatedModLoaders = allNewModLoadersAndVersions.filter { newModLoader ->
            allOldModLoadersAndVersions.any { oldModLoader ->
                oldModLoader.first == newModLoader.first && oldModLoader.second != newModLoader.second
            }
        }

        if (verboseOpt)
        {
            // Find the max length of the loader name and old version to align output arrow
            val maxLength = updatedModLoaders.maxOfOrNull { oldModLoader ->
                "${oldModLoader.first.replaceFirstChar { it.uppercase() }} ${oldModLoader.second}".length
            } ?: 0

            // Same mod loader in both sets but with different versions
            return updatedModLoaders.map { newModLoader ->
                val oldModLoader = allOldModLoadersAndVersions.first { it.first == newModLoader.first }
                val oldModLoaderNameAndVersion =
                    "${oldModLoader.first.replaceFirstChar { it.uppercase() }} ${oldModLoader.second}"
                val newModLoaderNameAndVersion =
                    "${newModLoader.first.replaceFirstChar { it.uppercase() }} ${newModLoader.second}"

                // Add padding to align output arrow
                oldModLoaderNameAndVersion.padEnd(maxLength) + " -> " + newModLoaderNameAndVersion
            }
        }
        return updatedModLoaders.map { loader -> loader.first.replaceFirstChar { it.uppercase() } }
    }

    private fun getUpdatedProjects(allOldProjects: List<Project>, allNewProjects: List<Project>): MutableList<String>
    {
        val updatedProjects = mutableListOf<String>()
        val verboseUpdatedFiles = mutableMapOf<String, String>()

        for (newProject in allNewProjects)
        {
            /** We only care about projects, which previously also existed **/
            val oldProject = allOldProjects.find { it.isAlmostTheSameAs(newProject) } ?: continue

            /** If the project files are not identical, means that files have changed **/
            if (oldProject.files == newProject.files) continue

            /** On multi-loader modpacks, the mod might have been added for another loader, causing the
             * files key to have changed without an actual update.
             **/
            val oldFileHash = oldProject.files.firstOrNull()?.hashes?.get("sha1")
            val newFileHash = newProject.files.firstOrNull()?.hashes?.get("sha1")
            if (oldFileHash == newFileHash) continue

            if (verboseOpt)
            {
                val oldFileName = oldProject.files.firstOrNull()?.fileName
                val newFileName = newProject.files.firstOrNull()?.fileName
                val fileNamesNotNullOrEmpty = !oldFileName.isNullOrEmpty() && !newFileName.isNullOrEmpty()

                if (fileNamesNotNullOrEmpty) verboseUpdatedFiles["$oldFileName"] = "$newFileName"
            }
            else
            {
                updatedProjects.add("${newProject.name.values.firstOrNull()}")
            }
        }
        if (verboseOpt)
        {
            val maxOldFileLength = verboseUpdatedFiles.keys.maxOfOrNull { it.length } ?: 0
            for ((oldFileName, newFileName) in verboseUpdatedFiles)
            {
                updatedProjects.add("${oldFileName.padEnd(maxOldFileLength)} -> $newFileName")
            }
        }
        return updatedProjects
    }

    private fun printDiffChangesToTerminal(
        addedMCVersions: Set<String>,
        removedMCVersions: Set<String>,
        addedModLoaders: Set<String>,
        removedModLoaders: Set<String>,
        updatedModLoaders: Set<String>,
        addedProjects: Set<String>,
        removedProjects: Set<String>,
        updatedProjects: List<String>
    )
    {
        addedMCVersions.forEach { terminal.success("+ $it") }
        removedMCVersions.forEach { terminal.danger("- $it") }
        addedModLoaders.forEach { terminal.success("+ $it") }
        removedModLoaders.forEach { terminal.danger("- $it") }
        updatedModLoaders.forEach { terminal.info("! $it") }
        addedProjects.forEach { terminal.success("+ $it") }
        removedProjects.forEach { terminal.danger("- $it") }
        updatedProjects.forEach { terminal.info("! $it") }
    }

    private fun writeDiffChangesToFile(
        addedMCVersions: Set<String>,
        removedMCVersions: Set<String>,
        addedModLoaders: Set<String>,
        removedModLoaders: Set<String>,
        updatedModLoaders: Set<String>,
        addedProjects: Set<String>,
        removedProjects: Set<String>,
        updatedProjects: List<String>,
        didMCVersionsChange: Boolean,
        didModLoaderChange: Boolean,
        didProjectsChange: Boolean
    )
    {
        if (markdownDiffOpt != null)
        {
            val file = File(markdownDiffOpt)

            file.createNewFile()
            file.outputStream().close()

            file.appendText("```diff\n")
            if (didMCVersionsChange)
            {
                addedMCVersions.forEach { file.appendText("+ $it\n") }
                removedMCVersions.forEach { file.appendText("- $it\n") }
                if (didModLoaderChange || didProjectsChange) file.appendText("\n")
            }
            if (didModLoaderChange)
            {
                addedModLoaders.forEach { file.appendText("+ $it\n") }
                removedModLoaders.forEach { file.appendText("- $it\n") }
                updatedModLoaders.forEach { file.appendText("! $it\n") }
                if (didProjectsChange) file.appendText("\n")
            }
            if (didProjectsChange)
            {
                addedProjects.forEach { file.appendText("+ $it\n") }
                removedProjects.forEach { file.appendText("- $it\n") }
                updatedProjects.forEach { file.appendText("! $it\n") }
            }
            file.appendText("```\n")
        }
        if (markdownOpt != null)
        {
            val file = File(markdownOpt)
            file.createNewFile()
            file.outputStream().close()

            val mainHeader = if (headerSizeOpt > 0) "${"#".repeat(headerSizeOpt)} " else ""
            val subHeader = if (headerSizeOpt > 0) "${"#".repeat(headerSizeOpt + 1)} " else ""

            if (didMCVersionsChange)
            {
                file.appendText("${mainHeader}Minecraft\n\n")
                if (addedMCVersions.isNotEmpty())
                {
                    file.appendText("${subHeader}Added\n\n")
                    addedMCVersions.forEach { file.appendText("- $it\n") }
                    if (removedMCVersions.isNotEmpty() || didModLoaderChange || didProjectsChange) file.appendText("\n")
                }
                if (removedMCVersions.isNotEmpty())
                {
                    file.appendText("${subHeader}Removed\n\n")
                    removedMCVersions.forEach { file.appendText("- $it\n") }
                    if (didModLoaderChange || didProjectsChange) file.appendText("\n")
                }
            }
            if (didModLoaderChange)
            {
                file.appendText("${mainHeader}Loaders\n\n")
                if (addedModLoaders.isNotEmpty())
                {
                    file.appendText("${subHeader}Added\n\n")
                    addedModLoaders.forEach { file.appendText("- $it\n") }
                    if (removedModLoaders.isNotEmpty() || updatedModLoaders.isNotEmpty() || didProjectsChange) file.appendText(
                        "\n"
                    )
                }
                if (removedModLoaders.isNotEmpty())
                {
                    file.appendText("${subHeader}Removed\n\n")
                    removedModLoaders.forEach { file.appendText("- $it\n") }
                    if (updatedModLoaders.isNotEmpty() || didProjectsChange) file.appendText("\n")
                }
                if (updatedModLoaders.isNotEmpty())
                {
                    file.appendText("${subHeader}Updated\n\n")
                    updatedModLoaders.forEach { file.appendText("- $it\n") }
                    if (didProjectsChange) file.appendText("\n")
                }
            }

            if (didProjectsChange)
            {
                file.appendText("${mainHeader}Projects\n\n")
                if (addedProjects.isNotEmpty())
                {
                    file.appendText("${subHeader}Added\n\n")
                    addedProjects.forEach { file.appendText("- $it\n") }
                    if (removedProjects.isNotEmpty() || updatedProjects.isNotEmpty()) file.appendText("\n")
                }
                if (removedProjects.isNotEmpty())
                {
                    file.appendText("${subHeader}Removed\n\n")
                    removedProjects.forEach { file.appendText("- $it\n") }
                    if (updatedProjects.isNotEmpty()) file.appendText("\n")
                }
                if (updatedProjects.isNotEmpty())
                {
                    file.appendText("${subHeader}Updated\n\n")
                    updatedProjects.forEach { file.appendText("- $it\n") }
                }
            }
        }
    }
}
