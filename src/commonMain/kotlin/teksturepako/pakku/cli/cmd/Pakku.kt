package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.HelpFormatter.ParameterHelp.Argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.VERSION
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.searchUpForWorkingPath
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.arg.overrideYes
import teksturepako.pakku.cli.ui.pInfo
import teksturepako.pakku.cli.ui.pSuccess
import teksturepako.pakku.debugMode
import teksturepako.pakku.io.tryOrNull
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.system.exitProcess

class Pakku : CliktCommand()
{
    init
    {
        installMordantMarkdown()
        versionOption(VERSION, help = "Get pakku version")
        completionOption(help = "Generate autocompletion scripts")
        eagerOption("--generate-docs", help = "Generate docs for CLI commands") {
            runBlocking { generateDocs(this@eagerOption) }
        }
    }

    private val yesFlag: Boolean by option("-y", "--yes")
        .help("Overwrite every prompt to 'yes' without asking")
        .flag()

    private val debugFlag: Boolean by option("--debug")
        .help("Enable additional debug logging")
        .flag()

    private val workingPathOpt: String? by option("--working-path", metavar = "<path>")
        .help("Change the working path of Pakku")

    override fun run()
    {
        overrideYes = yesFlag
        debugMode = debugFlag

        if (workingPathOpt != null)
        {
            workingPath = workingPathOpt as String
        }
        else if (!LockFile.exists()) runBlocking {
            searchUpForWorkingPath()?.let { workingPath = it }
        }
    }
}

@OptIn(ExperimentalPathApi::class)
suspend fun CliktCommand.generateDocs(context: OptionTransformContext)
{
    val outputDirectory = Path(workingPath, PAKKU_DIR, "docs")

    outputDirectory.tryOrNull {
        it.deleteRecursively()
        it.createParentDirectories()
        it.createDirectory()
    }

    terminal.pInfo("Generating docs:")

    val commands: List<Pair<CoreCliktCommand, List<CoreCliktCommand>>> = this.registeredSubcommands().map { cmd ->
        val subcmds = cmd.registeredSubcommands()

        cmd to subcmds
    }

    commands.forEach { (command, subcommands) ->
        this.createDocsFile(outputDirectory, command, context, subcommands).onSuccess {
            terminal.pSuccess(it.toString())
        }

        subcommands.forEach { subcommand ->
            this.createDocsFile(outputDirectory, subcommand, context, parentCommand = command).onSuccess {
                terminal.pSuccess(it.toString())
            }
        }
    }

    exitProcess(0)
}

fun CliktCommand.createDocsFile(
    outputDirectory: Path, command: CoreCliktCommand, context: OptionTransformContext,
    subcommands: List<CoreCliktCommand> = listOf(), parentCommand: CoreCliktCommand? = null
): Result<Path, String>
{
    val cmdFileName = buildString {
        append("${this@createDocsFile.commandName}-") // Root command
        if (parentCommand != null) append("${parentCommand.commandName}-") // Parent command
        append(command.commandName) // Current command
        append(".md")
    }

    val cmdName = command.commandName
    val cmdHelp = command.help(context.context)

    val args: List<Argument> = command.allHelpParams().filterIsInstance<Argument>()

    val cmdParams = "[&lt;options&gt;] " + args.joinToString(" ") { arg ->
        buildString {
            if (!arg.required) append("[")
            append("&lt;${arg.name.lowercase()}&gt;")
            if (!arg.required) append("]")
            if (arg.repeatable) append("...")
        }
    }

    val opts = command.registeredOptions().filter { "--help" !in it.names }
    val helpOpt = command.registeredOptions().find { "--help" in it.names }

    val text = buildString {
        append("# pakku ")
        if (parentCommand != null) append("${parentCommand.commandName} ")
        append("$cmdName\n")

        append("\n")
        append("$cmdHelp\n")
        append("\n")
        append("## Usage\n")
        append("\n")
        append("<snippet id=\"snippet-cmd\">\n")
        append("\n")

        append("<var name=\"cmd\">")
        if (parentCommand != null) append("${parentCommand.commandName} ")
        append("$cmdName</var>\n")

        append("<var name=\"params\">$cmdParams</var>\n")
        append("<include from=\"_template_cmd.md\" element-id=\"template-cmd\"/>\n")
        append("\n")
        append("</snippet>\n")
        append("\n")
        if (subcommands.isNotEmpty())
        {
            append("\n")
            append("## Subcommands\n")
            append("\n")
            for (subcommand in subcommands)
            {
                val subcommandFileName = buildString {
                    append("${this@createDocsFile.commandName}-") // Root command
                    append("$cmdName-") // Parent command
                    append(subcommand.commandName) // Subcommand
                    append(".md")
                }

                append("[`${subcommand.commandName}`]($subcommandFileName)\n")
                append(": ${subcommand.help(context.context)}\n")
                append("\n")
            }
        }
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
                val optHelp = opt.optionHelp(context.context).ifBlank { "The $optName option" }

                append("$optName\n")
                append(": $optHelp\n")
                append("\n")
            }
            append("</snippet>\n")
            append("\n")
        }
        helpOpt?.let { opt ->
            val optName = opt.names.joinToString { "`$it`" }
            val optHelp = "Show the help message and exit"

            append("$optName\n")
            append(": $optHelp\n")
        }
        append("\n")
        append("</snippet>\n")
        append("")
    }

    val cmdFile = Path(outputDirectory.pathString, cmdFileName)
    cmdFile.createFile()
    cmdFile.writeText(text)

    return Ok(cmdFile)
}
