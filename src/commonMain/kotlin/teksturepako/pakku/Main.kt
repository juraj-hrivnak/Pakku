package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.CredentialsFile
import teksturepako.pakku.api.http.pakkuClient
import teksturepako.pakku.api.pakku
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.cli.cmd.Set
import teksturepako.pakku.cli.fixSystemOutEncoding
import teksturepako.pakku.cli.ui.CliConfig
import teksturepako.pakku.cli.ui.CliThemes
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    val credentials = runBlocking { CredentialsFile.readToResult() }
        .onFailure { error -> debug { println(error.rawMessage) } }
        .get()

    pakku {
        curseForge(apiKey = System.getenv("CURSEFORGE_API_KEY") ?: credentials?.curseForgeApiKey)
        gitHub(accessToken = System.getenv("GITHUB_ACCESS_TOKEN") ?: credentials?.gitHubAccessToken)
        withUserAgent("Pakku/$VERSION (github.com/juraj-hrivnak/Pakku)")
    }

    val utf8Supported = fixSystemOutEncoding()

    // Read 'cli-config.json'
    val cliConfig = runBlocking { CliConfig.readToResult() }
        .onFailure { error -> debug { println(error.rawMessage) } }
        .get()

    Pakku().context {
        terminal = cliConfig?.toTerminal()
            ?: Terminal(theme = if (utf8Supported) CliThemes.Default else CliThemes.Ascii)
    }.subcommands(
        Init(), Import(), Add(), Rm(), Insp(), Cfg(), Set(), Status(), Update(), Ls(), Fetch(), Sync(), Link(),
        Export(), Diff(), Remote(), Credentials()
    ).main(args)

    debug { println("Program arguments: ${args.joinToString()}") }

    pakkuClient.close()
    exitProcess(0)
}
