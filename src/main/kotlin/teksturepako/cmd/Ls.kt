package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.data.PakkuLock
import teksturepako.platforms.Multiplatform

class Ls : CliktCommand("List mods")
{
    override fun run() = runBlocking {
        PakkuLock.get { data ->
            data.projects.map { project ->
                async {
                    Multiplatform.requestProject(project.slug)
                }
            }
        }.forEach {
            val project = it.await()

            if (project != null)
            {
                terminal.success(project.slug)
            }
        }
        echo()
    }
}