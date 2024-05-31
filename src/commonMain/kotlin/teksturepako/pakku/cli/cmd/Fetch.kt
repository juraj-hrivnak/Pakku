package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.widgets.Spinner
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.PAKKU_DIR
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug
import teksturepako.pakku.io.createHash
import java.io.File

class Fetch : CliktCommand("Fetch projects to your modpack folder")
{
    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        var fetched = false
        val ignored = mutableMapOf<ProjectType, List<File>>()
        val jobs = mutableListOf<Job>()

        // Progress bar
        val progress = terminal.progressAnimation {
            text("Fetching ")
            spinner(Spinner.Dots())
            percentage()
            padding = 0
        }

        val maxSize: AtomicLong = atomic(0L)

        val projectFiles = lockFile.getAllProjects()
            .map { project ->
                for (platform in Multiplatform.platforms)
                {
                    return@map Result.success(project.type to (project.getFilesForPlatform(platform)
                        .takeIf { it.isNotEmpty() }
                        ?.first() ?: break)
                    )
                }
                return@map Result.failure(PakkuException("No files found for ${project.slug}"))
            }.mapNotNull { result ->
                result.getOrElse {
                    terminal.danger(it.message)
                    echo()
                    null
                }
            }

        for ((projectType, projectFile) in projectFiles)
        {
            val outputFile = File("$workingPath/${projectType.folderName}/${projectFile.fileName}")
                .also { ignored[projectType] = ignored[projectType]?.let { list -> list + it } ?: listOf(it) }

            val parentFolder = File("$workingPath/${projectType.folderName}")

            // Skip to next if output file exists
            if (outputFile.exists()) continue

            jobs += launch(Dispatchers.IO) {
                maxSize += projectFile.size.toLong()
                val prevBytes: AtomicLong = atomic(0L)

                // Download
                val deferred = async {
                    Http().requestByteArray(projectFile.url!!) { bytesSentTotal, _ ->
                        progress.advance(bytesSentTotal - prevBytes.value)
                        progress.updateTotal(maxSize.value)

                        prevBytes.getAndSet(bytesSentTotal)
                    } to projectFile
                }

                // Create parent folders
                if (!parentFolder.exists()) parentFolder.mkdirs()

                val (bytes, file) = deferred.await()

                if (bytes != null) try
                {
                    if (file.hashes != null)
                    {
                        for ((hashType, originalHash) in file.hashes)
                        {
                            val newHash = createHash(hashType, bytes)
                            debug { println("$hashType: $originalHash : $newHash") }

                            if (originalHash != newHash)
                            {
                                terminal.danger("${outputFile.name} failed to mach hash $hashType")
                                terminal.danger("File will not be saved")
                                return@launch
                            }
                            else continue
                        }
                    }

                    // Write to file
                    outputFile.writeBytes(bytes)
                    terminal.success("${outputFile.name} saved")

                    fetched = true
                } catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
        }

        jobs.joinAll()

        if (fetched)
        {
            progress.clear()
            terminal.success("Projects successfully fetched")
            echo()
        }

        // -- OVERRIDES --

        val projectOverrides = Overrides.getProjectOverrides()
        var synced = false

        projectOverrides.map { projectOverride ->
            launch {
                val file = File("$workingPath/${projectOverride.projectType.folderName}/${projectOverride.fileName}")
                if (!file.exists()) runCatching {
                    file.parentFile.mkdir()
                    File(
                        "$workingPath/$PAKKU_DIR/${projectOverride.overrideType.folderName}/" +
                                "${projectOverride.projectType.folderName}/${projectOverride.fileName}"
                    ).copyTo(file)

                    terminal.info("${projectOverride.fileName} synced")
                    synced = true
                }
            }
        }.joinAll()

        if (synced)
        {
            terminal.info("Project overrides successfully synced")
            echo()
        }

        // -- OLD FILES --

        var removed = false

        val ignoredProjOverrFileNames = projectOverrides.map { projectOverride ->
            projectOverride.fileName
        }

        ignored.map { (projectType, ignoredFiles) ->
            launch(Dispatchers.IO) {
                val ignoredNames = ignoredFiles.map { it.name }

                File("$workingPath/${projectType.folderName}").listFiles()
                    .filter { file ->
                        file.name !in ignoredNames
                                && file.name !in ignoredProjOverrFileNames
                                && file.extension in listOf("jar", "zip")
                                && file.parentFile.name == projectType.folderName
                    }.forEach { file ->
                        file.delete()
                        terminal.warning("${file.name} deleted")
                        removed = true
                    }
            }
        }.joinAll()

        if (removed)
        {
            terminal.warning("Old project files successfully deleted")
            echo()
        }
    }
}