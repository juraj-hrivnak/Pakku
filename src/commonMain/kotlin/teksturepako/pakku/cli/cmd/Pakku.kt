package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import teksturepako.pakku.VERSION
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.overrideYes
import teksturepako.pakku.debugMode

class Pakku : CliktCommand()
{
    init {
        versionOption(VERSION, help = "Get pakku version")
    }

    private val yesFlag: Boolean by option(
        "-y", "--yes",
        help = "Overwrite every prompt to 'yes' without asking"
    ).flag()

    private val debugFlag: Boolean by option("--debug", help = "Enable additional debug logging").flag()

    private val workingPathOpt: String? by option(
        "--working-path",
        help = "Change the working path of Pakku",
        metavar = "<path>"
    )

    override fun run()
    {
        overrideYes = yesFlag
        debugMode = debugFlag
        workingPathOpt?.let { workingPath = it }
    }
}
