package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.michaelbull.result.fold
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.remote.remoteRemove
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pError

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

            remoteRemove().fold(
                success = { terminal.pDanger("Remote removed") },
                failure = { terminal.pError(it) }
            )

            echo()
        }
    }
}
