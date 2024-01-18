package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.widgets.Spinner
import kotlinx.coroutines.*
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class Fetch : CliktCommand("Fetch projects to your pack folder")
{
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readToResult().fold(
            onSuccess = { it },
            onFailure = {
                terminal.danger(it.message)
                echo()
                return@runBlocking
            }
        )

        var fetched = false
        val ignored = mutableListOf<Path>()
        val jobs = mutableListOf<Job>()

        // Progress bar
        val progress = terminal.progressAnimation {
            text("Fetching ")
            spinner(Spinner.Dots())
            padding = 0
        }

        var maxSize: Long = 0

        for (project in pakkuLock.getAllProjects())
        {
            var projectFile: ProjectFile? = null

            x@ for (platform in Multiplatform.platforms)
            {
                projectFile = project.getFilesForPlatform(platform)
                    .takeIf { it.isNotEmpty() }
                    ?.first() ?: continue@x
                break@x
            }

            if (projectFile?.url == null)
            {
                terminal.danger("No files found for ${project.slug}")
                echo()
                continue
            }

            val outputFile = Path(project.type.folderName, projectFile.fileName).also { ignored.add(it) }
            val parentFolder = Path(project.type.folderName).toFile()

            // Skip to next if output file exists
            if (outputFile.toFile().exists()) continue

            maxSize += projectFile.size

            // Download
            val deferred = async {
                Http().requestByteArray(projectFile.url!!) { _, _ ->
                    progress.updateTotal(maxSize)
                } to projectFile.size
            }

            jobs += launch(Dispatchers.IO) {
                // Create parent folders
                if (!parentFolder.exists()) parentFolder.mkdirs()

                val (bytes, fileSize) = deferred.await()

                if (bytes != null) try
                {
                    // Write to file
                    outputFile.writeBytes(bytes)
                    terminal.success("$outputFile saved")
                    progress.advance(fileSize.toLong())

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

        for (path in ignored)
        {
            launch(Dispatchers.IO) {
                if (path.parent.name in ProjectType.entries.map { it.folderName })
                {
                    path.parent.toFile().listFiles()
                        .filter {
                            it.name !in ignored.map { ignore -> ignore.name } && it.extension in listOf("jar", "zip")
                        }
                        .forEach(File::delete)
                }
            }
        }
    }
}