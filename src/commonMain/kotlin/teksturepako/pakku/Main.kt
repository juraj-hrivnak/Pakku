package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.executePakku
import teksturepako.pakku.api.initPakku
import teksturepako.pakku.api.platforms.CURSEFORGE_API_KEY
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.cli.cmd.Set
import teksturepako.pakku.cli.ui.CliConfig
import teksturepako.pakku.cli.ui.CliThemes
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    initPakku {
        curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY") ?: CURSEFORGE_API_KEY)
        withUserAgent("Pakku/$VERSION (github.com/juraj-hrivnak/Pakku)")
    }

    executePakku {
        // Read 'cli-config.json'
        val cliConfig = runBlocking { CliConfig.readToResult() }
            .onFailure { error -> debug { println(error.rawMessage) } }
            .get()

        Pakku().context {
            terminal = cliConfig?.toTerminal() ?: Terminal(theme = CliThemes.Default)
        }.subcommands(
            Init(), Import(), Add(), Rm(), Cfg(), Set(), Status(), Update(), Ls(), Fetch(), Sync(), Link(), Export(),
            Diff()
        ).main(args)

        // Check Modrinth's rate limit
        Modrinth.checkRateLimit()

        debug { CurseForge.checkApiKey() }
    }

    debug { println("Program arguments: ${args.joinToString()}") }

    exitProcess(0)
}
