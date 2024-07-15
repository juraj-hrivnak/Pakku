package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import teksturepako.pakku.VERSION
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.overrideYes
import teksturepako.pakku.debugMode
import java.io.File

class Pakku : CliktCommand()
{
    init {
        versionOption(VERSION, help = "Get pakku version")
        completionOption(help = "Generate autocompletion scripts")
        eagerOption(
            "--generate-docs", help = "Generate WriterSide docs for CLI commands", hidden = true
        ) {
            generateDocs(this)
        }
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

fun CliktCommand.generateDocs(context: OptionTransformContext)
{
    val outputFile = File("$workingPath/$PAKKU_DIR/docs")
    outputFile.mkdirs()

    this.registeredSubcommands().forEach { subcmd ->
        val cmdFileName = "${this.commandName}-${subcmd.commandName}.md"

        val cmdName = subcmd.commandName
        val cmdHelp = subcmd.commandHelp(context.context)

        val text = buildString {
            append("# pakku $cmdName\n")
            append("\n")
            append("$cmdHelp\n")
            append("\n")
            append("## Usage\n")
            append("\n")
            append("<snippet id=\"snippet-cmd\">\n")
            append("\n")
            append("<var name=\"cmd\">$cmdName</var>\n")
            append("<var name=\"help\"></var>\n")
            append("<include from=\"_template_cmd.md\" element-id=\"template-cmd\"/>\n")
            append("\n")
            append("</snippet>\n")
            append("\n")
            append("## Options\n")
            append("\n")
            append("<snippet id=\"snippet-options\">\n")
            append("\n")
            for (opt in subcmd.registeredOptions())
            {
                val optName = opt.names.joinToString { "`$it`" }
                val optHelp = opt.optionHelp(context.context)

                append("$optName\n")
                append(": $optHelp\n")
                append("\n")
            }
            append("</snippet>\n")
            append("")
        }

        val cmdFile = File(outputFile, cmdFileName)
        cmdFile.createNewFile()
        cmdFile.writeText(text)
    }
}
