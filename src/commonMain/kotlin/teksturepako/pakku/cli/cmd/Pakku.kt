package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.HelpFormatter.ParameterHelp.Argument
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.VERSION
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.overrideYes
import teksturepako.pakku.cli.ui.pInfo
import teksturepako.pakku.debugMode
import teksturepako.pakku.io.tryOrNull
import kotlin.io.path.*
import kotlin.system.exitProcess

class Pakku : CliktCommand()
{
    init {
        versionOption(VERSION, help = "Get pakku version")
        completionOption(help = "Generate autocompletion scripts")
        eagerOption(
            "--generate-docs", help = "Generate WriterSide docs for CLI commands", hidden = true
        ) {
            runBlocking { generateDocs(this@eagerOption) }
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

@OptIn(ExperimentalPathApi::class)
suspend fun CliktCommand.generateDocs(context: OptionTransformContext)
{
    val outputFile = Path(workingPath, PAKKU_DIR, "docs")

    outputFile.tryOrNull {
        it.deleteRecursively()
        it.createParentDirectories()
        it.createDirectory()
    }

    terminal.pInfo("Generating docs:")

    val commands = this.registeredSubcommands().fold(listOf<BaseCliktCommand<*>>()) { acc, cmd ->
        val subcmds = cmd.registeredSubcommands()

        acc + cmd + subcmds
    }

    commands.forEach { cmd ->
        val cmdFileName = "${this.commandName}-${cmd.commandName}.md"

        val cmdName = cmd.commandName
        val cmdHelp = cmd.commandHelp(context.context)

        val args: List<Argument> = cmd.allHelpParams().filterIsInstance<Argument>()

        val cmdParams = "[&lt;options&gt;] " + args.joinToString(" ") { arg ->
            buildString {
                if (!arg.required) append("[")
                append("&lt;${arg.name.lowercase()}&gt;")
                if (!arg.required) append("]")
                if (arg.repeatable) append("...")
            }
        }

        val opts = cmd.registeredOptions().filter { "--help" !in it.names }
        val helpOpt = cmd.registeredOptions().find { "--help" in it.names }

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
            append("<var name=\"params\">$cmdParams</var>\n")
            append("<include from=\"_template_cmd.md\" element-id=\"template-cmd\"/>\n")
            append("\n")
            append("</snippet>\n")
            append("\n")
            if (args.isNotEmpty())
            {
                append("## Arguments\n")
                append("\n")
                append("<snippet id=\"snippet-args\">\n")
                append("\n")
                for (arg in args)
                {
                    val argName = buildString {
                        append("`")
                        if (!arg.required) append("[")
                        append("<${arg.name.lowercase()}>")
                        if (!arg.required) append("]")
                        if (arg.repeatable) append("...")
                        append("`")
                    }
                    val argHelp = arg.help.ifBlank { "The `${arg.name.lowercase()}` argument" }

                    append("$argName\n")
                    append(": $argHelp\n")
                    append("\n")
                }
                append("</snippet>\n")
                append("\n")
            }
            append("## Options\n")
            append("\n")
            append("<snippet id=\"snippet-options-all\">\n")
            append("\n")
            if (opts.isNotEmpty())
            {
                append("<snippet id=\"snippet-options\">\n")
                append("\n")
                for (opt in opts)
                {
                    val optName = opt.names.joinToString { "`$it`" }
                    val optHelp = opt.optionHelp(context.context)

                    append("$optName\n")
                    append(": $optHelp\n")
                    append("\n")
                }
                append("</snippet>\n")
                append("\n")
            }
            helpOpt?.let { opt ->
                val optName = opt.names.joinToString { "`$it`" }
                val optHelp = opt.optionHelp(context.context)

                append("$optName\n")
                append(": $optHelp\n")
                append("\n")
            }
            append("</snippet>\n")
            append("")
        }

        val cmdFile = Path(outputFile.pathString, cmdFileName)
        cmdFile.createFile()
        cmdFile.writeText(text)

        terminal.pInfo(cmdFile.pathString)
    }

    exitProcess(0)
}
