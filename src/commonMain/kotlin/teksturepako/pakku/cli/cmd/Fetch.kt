package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.widgets.Spinner
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.*
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.PAKKU_DIR
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.ui.prefixed
import teksturepako.pakku.cli.ui.processErrorMsg
import teksturepako.pakku.io.createHash
import kotlin.io.path.*
import com.github.michaelbull.result.runCatching as runCatching

class Fetch : CliktCommand("Fetch projects to your modpack folder")
{
    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val progressBar = terminal.progressAnimation {
            text("Fetching ")
            spinner(Spinner.Dots())
            percentage()
            padding = 0
        }

        val projectFiles = retrieveProjectFiles(lockFile, Multiplatform.platforms.first()).mapNotNull { result ->
            result.getOrElse {
                terminal.println(processErrorMsg(it))
                null
            }
        }

        projectFiles.fetch(
            onError = { error ->
                if (error !is AlreadyExists) terminal.println(processErrorMsg(error))
            },
            onProgress = { advance, total ->
                progressBar.advance(advance)
                progressBar.updateTotal(total)
            },
            onSuccess = { projectFile, _ ->
                terminal.success(prefixed("${projectFile.getPath()} saved"))
            },
            lockFile
        )

        // -- OVERRIDES --

        val projectOverrides = Overrides.getProjectOverrides()

        val projOverrideHashes = projectOverrides.map { projectOverride ->
            async {
                val file = Path(workingPath, projectOverride.projectType.folderName, projectOverride.fileName)
                if (!file.exists()) runCatching {
                    file.createParentDirectories()
                    Path(
                        workingPath,
                        PAKKU_DIR,
                        projectOverride.overrideType.folderName,
                        projectOverride.projectType.folderName,
                        projectOverride.fileName
                    ).copyTo(file)
                    terminal.info(prefixed("${projectOverride.fileName} synced"))
                }
                createHash("sha1", file.readBytes())
            }
        }.awaitAll()

        // -- OLD FILES --

        val oldFiles = ProjectType.entries
            .filterNot { it == ProjectType.WORLD }
            .mapNotNull { projectType ->
                val folder = Path(workingPath, projectType.folderName)
                if (folder.notExists()) return@mapNotNull null
                runCatching { folder.listDirectoryEntries() }.get()
            }.flatMap { entry ->
                entry.filterNot { file ->
                    val bytes = runCatching { file.readBytes() }.get()
                    val fileHash = bytes?.let { createHash("sha1", it) }

                    fileHash in projOverrideHashes || !projectFiles.all { projectFile ->
                        projectFile.hashes?.get("sha1").let {
                            if (it == null) return@let false
                            fileHash != it
                        }
                    } || file.extension !in listOf("jar", "zip")
                }
            }

        oldFiles.map {
            launch(Dispatchers.IO) {
                it.deleteIfExists()
                terminal.danger(prefixed("${it.name} deleted"))
            }
        }.joinAll()

        progressBar.clear()
    }
}