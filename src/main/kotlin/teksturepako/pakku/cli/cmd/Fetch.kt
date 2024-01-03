package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.animation.ProgressAnimation
import com.github.ajalt.mordant.animation.progressAnimation
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.CurseForge.bodyIfOK
import teksturepako.pakku.api.platforms.CurseForge.checkLimit
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
    @OptIn(ExperimentalCoroutinesApi::class)
    private val downloadDispatcher = Dispatchers.Default.limitedParallelism(10)

    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        var fetched = false
        val ignored = mutableListOf<Path>()
        val jobs = mutableListOf<Job>()
        val progressBars = mutableListOf<ProgressAnimation>()
        val mutex = Mutex()

        // Progress bar
        val progress = terminal.progressAnimation {
            text("Fetching...")
            percentage()
            progressBar()
            padding = 0
        }.also { progressBars.add(it) }

        for (project in pakkuLock.getAllProjects())
        {
            var projectFile: ProjectFile? = null

            platforms@ for (platform in Multiplatform.platforms)
            {
                projectFile = project.getFilesForPlatform(platform)
                    .takeIf { it.isNotEmpty() }
                    ?.first() ?: continue@platforms
                break@platforms
            }

            if (projectFile?.url == null)
            {
                terminal.danger("No files found for ${project.files.map { it.type }}")
                echo()
                continue
            }

            val outputFile = Path(project.type.folderName, projectFile.fileName).also { ignored.add(it) }
            val parentFolder = Path(project.type.folderName).toFile()

            // Skip to next if output file exists
            if (outputFile.toFile().exists()) continue

            mutex.withLock {
                // Download
                val deferred = async(downloadDispatcher) {
                    client.get(projectFile.url!!) {
                        onDownload { bytesSentTotal, contentLength ->
                            if (bytesSentTotal > 1)
                            {
                                if (bytesSentTotal + 64 > contentLength)
                                {
                                    progress.update(contentLength)
                                } else
                                {
                                    progress.start()
                                    progress.updateTotal(contentLength)
                                    progress.update(bytesSentTotal)
                                }
                            }
                        }
                    }.checkLimit().bodyIfOK() as ByteArray?
                }

                jobs += launch(downloadDispatcher) {
                    // Create parent folders
                    if (!parentFolder.exists()) parentFolder.mkdirs()

                    val bytes = deferred.await()

                    if (bytes != null) try
                    {
                        // Write to file
                        outputFile.writeBytes(bytes)
                        fetched = true
                    } catch (e: Exception)
                    {
                        e.printStackTrace()
                    }
                }
            }
        }

        jobs.joinAll()
        progressBars.forEach { it.clear() }

        if (fetched)
        {
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
                            it.name !in ignored.map { ignore -> ignore.name }
                                    && it.extension in listOf("jar", "zip")
                        }
                        .forEach(File::delete)
                }
            }
        }
    }
}