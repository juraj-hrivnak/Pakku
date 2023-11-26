package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Multiplatform
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

class Fetch : CliktCommand()
{
    override fun run() = runBlocking {
        var fetched = false

        for (it in PakkuLock.get { data ->
            data.projects.map { project ->
                async {
                    Multiplatform.requestProjectFiles(
                        data.mcVersions,
                        data.loaders,
                        project.slug[CurseForge.serialName]!!
                    )
                }
            }
        })
        {
            val project = it.await()

            if (project != null)
            {
                val projectFile = project.files.run {
                    if (this.isNotEmpty()) first() else null
                }

                if (projectFile != null)
                {
                    val outputFile = Path(project.type.folderName, projectFile.fileName)
                    val folder = Path(project.type.folderName).toFile()

                    // Skip to next if output file exists
                    if (outputFile.toFile().exists()) continue

                    // Create parent folders
                    if (!folder.exists()) folder.mkdirs()

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
            }
        }
        if (fetched) echo()
    }
}