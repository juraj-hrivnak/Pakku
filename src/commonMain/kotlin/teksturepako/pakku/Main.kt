package teksturepako.pakku

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.http.client
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.*
import teksturepako.pakku.io.exitPakku
import java.io.File

fun main(args: Array<String>)
{
    println()

    Pakku().context {
        terminal = Terminal(
            theme = Theme.Default, ansiLevel = AnsiLevel.TRUECOLOR, interactive = true
        )
    }.subcommands(
        Init(), Import(), Set(), Add(), Rm(), Update(), Ls(), Fetch(), Link(), Export(), Diff(), Version()
    ).main(args)

    // Check Modrinth's rate limit
    Modrinth.checkRateLimit()

    println("Program arguments: ${args.joinToString()}")

    runBlocking {
        val lockFile = LockFile.readToResult().getOrNull() ?: return@runBlocking

        val files = lockFile.getAllProjects().map {
            "${it.type.folderName}/${it.files.firstOrNull()?.fileName}"
        }

        val bytes = files.mapNotNull { runCatching { File(it).readBytes() }.getOrNull() }

        val test = CurseForge.requestMultipleProjectsWithFilesFromBytes(lockFile.getMcVersions(), bytes)

        println(test)
    }

    // Close http client & exit program
    client.close()
    exitPakku(0)
}
