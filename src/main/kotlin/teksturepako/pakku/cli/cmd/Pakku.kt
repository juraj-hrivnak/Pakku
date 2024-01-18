package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import teksturepako.pakku.debugMode

class Pakku : CliktCommand()
{
    private val debugFlag: Boolean by option("--debug", help = "Enable additional debug logging").flag()

    override fun run()
    {
        debugMode = debugFlag
    }
}
