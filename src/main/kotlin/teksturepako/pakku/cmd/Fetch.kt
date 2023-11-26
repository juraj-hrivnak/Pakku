package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.writeBytes

class Fetch : CliktCommand()
{
    override fun run() = runBlocking {
        var fetched = false
        val ignore = mutableListOf<Path>()

        for (project in PakkuLock.getAllProjects())
        {
            var projectFile: ProjectFile? = null

            platforms@ for (platform in Multiplatform.platforms)
            {
                projectFile = project.getFilesForPlatform(platform)
                    .takeIf { it.isNotEmpty() }
                    ?.first() ?: continue@platforms
                break@platforms
            }

            if (projectFile == null)
            {
                terminal.danger("No files found for ${project.files.map { it.type }}")
                echo()
                continue
            }

            val outputFile = Path(project.type.folderName, projectFile.fileName).also { ignore.add(it) }
            val parentFolder = Path(project.type.folderName).toFile()

            // Skip to next if output file exists
            if (outputFile.toFile().exists()) continue

            // Create parent folders
            if (!parentFolder.exists()) parentFolder.mkdirs()

            withContext(Dispatchers.IO) {
                // Download
                val bytes = projectFile.url?.run { Http().requestByteArray(this) }
                if (bytes != null) try
                {
                    // Write to file
                    outputFile.writeBytes(bytes)
                    terminal.success(outputFile)
                    fetched = true
                } catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
        }
        ignore.forEach { path ->
            if (path.parent.name in ProjectType.entries.map { it.folderName })
            {
                path.parent.toFile().listFiles()
                    .filter { it.name !in ignore.map { ignore -> ignore.name } }
                    .forEach { it.delete() }
            }
        }
        if (fetched) echo()
    }
}