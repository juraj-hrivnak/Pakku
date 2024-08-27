package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.completion.completionOption
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
import java.io.File
import kotlin.io.path.*
import kotlin.system.exitProcess

class Pakku : CliktCommand()
{
    init
    {
        versionOption(VERSION, help = "Get pakku version")
        completionOption(help = "Generate autocompletion scripts")
        eagerOption(
            "--generate-docs", help = "Generate WriterSide docs for CLI commands", hidden = true
        ) {
            runBlocking { generateDocs(this@eagerOption) }
        }
    }

    private val yesFlag: Boolean by option(
        "-y", "--yes", help = "Overwrite every prompt to 'yes' without asking"
    ).flag()

    private val debugFlag: Boolean by option("--debug", help = "Enable additional debug logging").flag()

    private val workingPathOpt: String? by option(
        "--working-path", help = "Change the working path of Pakku", metavar = "<path>"
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

    this.registeredSubcommands().forEach { subcmd ->
        val cmdFileName = "${this.commandName}-${subcmd.commandName}.md"

        val cmdName = subcmd.commandName
        val cmdHelp = subcmd.commandHelp(context.context)

        val args: List<Argument> = subcmd.allHelpParams().filterIsInstance<Argument>()

        val cmdParams = "[&lt;options&gt;] " + args.joinToString(" ") { arg ->
            buildString {
                if (!arg.required) append("[")
                append("&lt;${arg.name.lowercase()}&gt;")
                if (!arg.required) append("]")
                if (arg.repeatable) append("...")
            }
        }

        val opts = subcmd.registeredOptions().filter { "--help" !in it.names }
        val helpOpt = subcmd.registeredOptions().find { "--help" in it.names }

        val text = buildString {
            append("# pakku $cmdName${File.separator}")
            append(File.separator)
            append("$cmdHelp${File.separator}")
            append(File.separator)
            append("## Usage${File.separator}")
            append(File.separator)
            append("<snippet id=\"snippet-cmd\">${File.separator}")
            append(File.separator)
            append("<var name=\"cmd\">$cmdName</var>${File.separator}")
            append("<var name=\"params\">$cmdParams</var>${File.separator}")
            append("<include from=\"_template_cmd.md\" element-id=\"template-cmd\"/>${File.separator}")
            append(File.separator)
            append("</snippet>${File.separator}")
            append(File.separator)
            if (args.isNotEmpty())
            {
                append("## Arguments${File.separator}")
                append(File.separator)
                append("<snippet id=\"snippet-args\">${File.separator}")
                append(File.separator)
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

                    append("$argName${File.separator}")
                    append(": $argHelp${File.separator}")
                    append(File.separator)
                }
                append("</snippet>${File.separator}")
                append(File.separator)
            }
            append("## Options${File.separator}")
            append(File.separator)
            append("<snippet id=\"snippet-options-all\">${File.separator}")
            append(File.separator)
            if (opts.isNotEmpty())
            {
                append("<snippet id=\"snippet-options\">${File.separator}")
                append(File.separator)
                for (opt in opts)
                {
                    val optName = opt.names.joinToString { "`$it`" }
                    val optHelp = opt.optionHelp(context.context)

                    append("$optName${File.separator}")
                    append(": $optHelp${File.separator}")
                    append(File.separator)
                }
                append("</snippet>${File.separator}")
                append(File.separator)
            }
            helpOpt?.let { opt ->
                val optName = opt.names.joinToString { "`$it`" }
                val optHelp = opt.optionHelp(context.context)

                append("$optName${File.separator}")
                append(": $optHelp${File.separator}")
                append(File.separator)
            }
            File("</snippet>${File.separator}")
            append("")
        }

        val cmdFile = Path(outputFile.pathString, cmdFileName)
        cmdFile.createFile()
        cmdFile.writeText(text)

        terminal.pInfo(cmdFile.pathString)
    }

    exitProcess(0)
}
