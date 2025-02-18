package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.pDanger

class RemoteRm : CliktCommand("rm")
{
    override fun help(context: Context) = "remove the remote from this modpack"

    override fun run() = runBlocking {
        coroutineScope {
            if (terminal.ynPrompt("Do you really want to remove the remote?"))
            {
                launch(Dispatchers.IO) {
                    Dirs.remoteDir.toFile().deleteRecursively()
                }.join()

                terminal.pDanger("Remote removed")
            }

            echo()
        }
    }
}
