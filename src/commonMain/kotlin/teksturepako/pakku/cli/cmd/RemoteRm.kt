package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.michaelbull.result.fold
import kotlinx.coroutines.*
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.io.tryToResult

class RemoteRm : CliktCommand("rm")
{
    override fun help(context: Context) = "remove the remote from this modpack"

    override fun run() = runBlocking {
        coroutineScope {
            if (!terminal.ynPrompt("Do you really want to remove the remote?"))
            {
                echo()
                return@coroutineScope
            }

            async(Dispatchers.IO) { Dirs.remoteDir.tryToResult { toFile().deleteRecursively() } }.await().fold(
                success = { terminal.pDanger("Remote removed") },
                failure = {
                    terminal.pDanger("Failed to remove the remote.")
                    terminal.pError(it)
                }
            )

            echo()
        }
    }
}
