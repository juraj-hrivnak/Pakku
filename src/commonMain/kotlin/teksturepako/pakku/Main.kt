package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.cli.cmd.Set
import teksturepako.pakku.cli.ui.CliConfig
import teksturepako.pakku.cli.ui.CliThemes
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    // Read 'cli-config.json'
    val cliConfig = runBlocking { CliConfig.readToResult() }
        .onFailure { error -> debug { println(error.rawMessage) } }
        .get()

    Pakku().context {
        terminal = cliConfig?.toTerminal() ?: Terminal(theme = CliThemes.Default)
    }.subcommands(
        Init(), Import(), Set(), Add(), Rm(), Cfg(), Status(), Update(), Ls(), Fetch(), Link(), Export(), Diff()
    ).main(args)

    // Check Modrinth's rate limit
    Modrinth.checkRateLimit()

    debug { println("Program arguments: ${args.joinToString()}") }

    // Close http client & exit program
    client.close()
    exitProcess(0)
}
