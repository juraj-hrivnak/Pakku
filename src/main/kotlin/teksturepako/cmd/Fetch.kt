package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.*
import teksturepako.data.PakkuLock
import teksturepako.http.Http
import teksturepako.platforms.CurseForge
import teksturepako.platforms.Multiplatform
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

class Fetch : CliktCommand()
{
    override fun run() = runBlocking {
        var fetched = false

        for (it in PakkuLock.get { data ->
            data.projects.map { project ->
                async {
                    Multiplatform.requestProjectFile(data.mcVersion, data.modLoader, project.slug)
                }
            }
        })
        {
            val project = it.await()

            if (project != null)
            {
                val projectFile = project.files[CurseForge.serialName]?.run {
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
                        }
                        catch (e: Exception)
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