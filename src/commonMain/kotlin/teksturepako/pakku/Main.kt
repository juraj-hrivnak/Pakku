package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.io.exitProcess

fun main(args: Array<String>)
{
    println()

    Pakku().context {
        terminal = Terminal(
            theme = Theme.Default, ansiLevel = AnsiLevel.TRUECOLOR, interactive = true
        )
    }.subcommands(
        Set(), Add(), Rm(), Update(), Ls(), Fetch(), Link(), Import()
    ).main(args)

    // Check Modrinth's rate limit
    Modrinth.checkRateLimit()

    println("Program arguments: ${args.joinToString()}")

    // Close client & exit program
    client.close()
    exitProcess(0)
}

