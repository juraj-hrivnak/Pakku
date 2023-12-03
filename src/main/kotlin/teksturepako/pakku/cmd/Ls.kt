package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform

class Ls : CliktCommand("List projects")
{
    override fun run() = runBlocking {
        for (project in PakkuLock.getAllProjects())
        {
            val name: String = project.name.values.first()
            val links: String = project.pakkuLinks.size.toString() + "\uD83D\uDD17"
            val platforms: String = Multiplatform.platforms.joinToString(" ") {
                if (project.isOnPlatform(it)) "${it.name}✔\uFE0F" else "${it.name}❌"
            }

            terminal.success("($links $platforms) $name")
        }
    }
}