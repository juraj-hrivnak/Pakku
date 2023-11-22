package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.data.PakkuLock

class Ls : CliktCommand("List mods")
{
    override fun run() = runBlocking {
        var listed = false

        for (it in PakkuLock.get { data ->
            if (data.projects.isNotEmpty()) listed = true
            data.projects.map { project ->
                async {
                    project.name
                }
            }
        }) {
            val name = it.await()

            terminal.success(name.values.first())
        }

        if (listed) echo()
    }
}