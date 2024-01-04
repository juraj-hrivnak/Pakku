package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform

class Ls : CliktCommand("List projects")
{
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readOrNew()

        for (project in pakkuLock.getAllProjects())
        {
            val name: String? = project.name.values.firstOrNull()
            val links: String = project.pakkuLinks.size.toString() + "\uD83D\uDD17"
            val platforms: String = Multiplatform.platforms.joinToString(" ") {
                if (project.hasFilesOnPlatform(it)) "${it.name}✔\uFE0F" else "${it.name}❌"
            }

            terminal.success("($links $platforms) $name")
        }
    }
}