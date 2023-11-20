package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.data.PakkuLock
import teksturepako.debug
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
                val projectFile = project.files[CurseForge.serialName]?.first().debug(::println)

                if (projectFile != null)
                {
                    val outputFile = Path(project.type.folderName, projectFile.fileName)
                    val folder = Path(project.type.folderName).toFile()

                    // Skip to next if output file exists
                    if (outputFile.toFile().exists()) continue

                    // Create parent folders
                    if (!folder.exists()) folder.mkdirs()

                    launch(Dispatchers.IO) {
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