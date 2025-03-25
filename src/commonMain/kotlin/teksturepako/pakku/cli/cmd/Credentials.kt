package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.eagerOption
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.cli.ui.pMsg

class Credentials : CliktCommand()
{
    override fun help(context: Context) = "Manage your credentials"

    init
    {
        eagerOption("--delete", help = "Delete all credentials") {
            runBlocking {
                CredentialsFile.delete()
                terminal.pMsg("Credentials deleted.")
                echo()
            }
        }
    }

    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = false
    override val printHelpOnEmptyArgs = true

    init
    {
        this.subcommands(CredentialsSet())
    }

    override fun run(): Unit = runBlocking {

    }
}