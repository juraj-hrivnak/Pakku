package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.cli.ui.pMsg

class Credentials : CliktCommand()
{
    override fun help(context: Context) = "Configure credentials"

    private val deleteFlag: Boolean by option("--delete")
        .help("delete all credentials")
        .flag()

    override val invokeWithoutSubcommand = true
    override val allowMultipleSubcommands = false
    override val printHelpOnEmptyArgs = true

    init
    {
        this.subcommands(CredentialsSet())
    }

    override fun run(): Unit = runBlocking {

        if (deleteFlag)
        {
            CredentialsFile.delete()
            terminal.pMsg("Credentials deleted.")
            echo()
        }
    }
}