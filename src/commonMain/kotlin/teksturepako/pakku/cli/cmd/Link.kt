package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pSuccess

class Link : CliktCommand("Link project to another project")
{
    private val projectOut: String by argument()
    private val projectIn: String by argument()

    override fun run() = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pDanger(it.message.toString())
            echo()
            return@runBlocking
        }

        val outId = lockFile.getProject(projectOut)?.pakkuId ?: return@runBlocking
        val project = lockFile.getProject(projectIn) ?: return@runBlocking
        lockFile.addPakkuLink(outId, project)
        terminal.pSuccess("$projectOut ($outId) linked to ${project.slug}")

        lockFile.write()
        echo()
    }
}