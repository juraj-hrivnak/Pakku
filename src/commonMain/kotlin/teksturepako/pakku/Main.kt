package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.cli.cmd.Set
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    Pakku().context {
        terminal = Terminal(theme = Theme.Default)
    }.subcommands(
        Init(), Import(), Set(), Add(), Rm(), Status(), Update(), Ls(), Fetch(), Run(), Link(), Export(), Diff()
    ).main(args)

    // Check Modrinth's rate limit
    Modrinth.checkRateLimit()

    debug { println("Program arguments: ${args.joinToString()}") }

    // Close http client & exit program
    client.close()
    exitProcess(0)
}
