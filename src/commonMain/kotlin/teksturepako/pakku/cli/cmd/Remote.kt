package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import kotlinx.coroutines.runBlocking

class Remote : CliktCommand()
{
    override fun help(context: Context) = "Create and install modpacks from remote"

    override val printHelpOnEmptyArgs = true

    private val urlArg by argument("url")
        .help("URL of the remote package or Git repository")

    private val args by findOrSetObject { mutableMapOf<String, String>() }

    init
    {
        this.subcommands(RemoteInstall())
    }

    override fun run() = runBlocking {
        // Pass args to the context
        args["urlArg"] = urlArg
    }
}