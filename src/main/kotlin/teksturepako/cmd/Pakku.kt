package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import teksturepako.debugMode

class Pakku : CliktCommand()
{
    private val debug: Boolean by option(help="Enable additional debug logging").flag()

    override fun run() {
        debugMode = debug
    }
}
