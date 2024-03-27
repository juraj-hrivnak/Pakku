package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import teksturepako.pakku.VERSION
import teksturepako.pakku.cli.ui.overrideYes
import teksturepako.pakku.debugMode

class Pakku : CliktCommand()
{
    private val versionFlag: Boolean by option(
        "--version",
        help = "Get pakku version",
        eager = true
    ).flag()

    private val yesFlag: Boolean by option(
        "-y", "--yes",
        help = "Overwrite every prompt to 'yes' without asking"
    ).flag()

    private val debugFlag: Boolean by option("--debug", help = "Enable additional debug logging").flag()

    override fun run()
    {
        if (versionFlag)
        {
            echo("Pakku version $VERSION")
            echo()
            return
        }
        overrideYes = yesFlag
        debugMode = debugFlag
    }
}
