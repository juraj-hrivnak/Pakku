package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.widgets.Spinner
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.AlreadyExists
import teksturepako.pakku.api.actions.fetch.fetch
import teksturepako.pakku.api.actions.fetch.retrieveProjectFiles
import teksturepako.pakku.api.actions.sync.getFileHashes
import teksturepako.pakku.api.actions.sync.sync
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.readProjectOverrides
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.ui.prefixed
import teksturepako.pakku.cli.ui.processErrorMsg
import teksturepako.pakku.io.createHash
import kotlin.io.path.*

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

        val projectFiles = retrieveProjectFiles(lockFile, Multiplatform.platforms).mapNotNull { result ->
            result.getOrElse {
                terminal.println(processErrorMsg(it))
                null
            }
        }

        projectFiles.fetch(
            onError = { error ->
                if (error !is AlreadyExists) terminal.println(processErrorMsg(error))
            },
            onProgress = { advance, total, sent ->
                progressBar.advance(advance)
                progressBar.updateTotal(total)

                if (sent >= total)
                {
                    progressBar.clear()
                    echo()
                }
            },
            onSuccess = { projectFile, _ ->
                terminal.success(prefixed("${projectFile.getPath()} saved"))
            },
            lockFile
        )

        // -- OVERRIDES --

        val projectOverrides = readProjectOverrides()

        projectOverrides.sync(
            onError = { error ->
                if (error !is AlreadyExists) terminal.println(processErrorMsg(error))
            },
            onSuccess = { projectOverride ->
                terminal.info(prefixed("${projectOverride.fullOutputPath} synced"))
            }
        ).joinAll()

        // -- OLD FILES --

        val oldFiles = ProjectType.entries
            .filterNot { it == ProjectType.WORLD }
            .mapNotNull { projectType ->
                val folder = Path(workingPath, projectType.folderName)
                if (folder.notExists()) return@mapNotNull null
                runCatching { folder.listDirectoryEntries() }.get()
            }.flatMap { entry ->
                entry.filter { file ->
                    val bytes = runCatching { file.readBytes() }.get()
                    val fileHash = bytes?.let { createHash("sha1", it) }

                    val projectFileNames = projectFiles.filter { projectFile ->
                        projectFile.hashes?.get("sha1") == null
                    }.map { projectFile ->
                        projectFile.fileName
                    }

                    file.extension in listOf("jar", "zip")
                            && fileHash !in projectOverrides.getFileHashes()
                            && file.name !in projectFileNames
                            && fileHash !in projectFiles.mapNotNull { projectFile ->
                                projectFile.hashes?.get("sha1")
                            }
                }
            }

        oldFiles.map {
            launch(Dispatchers.IO) {
                it.deleteIfExists()
                terminal.danger(prefixed("$it deleted"))
            }
        }.joinAll()
    }
}