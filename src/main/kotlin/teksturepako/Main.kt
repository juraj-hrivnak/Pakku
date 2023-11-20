package teksturepako

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.cmd.*
import teksturepako.cmd.Set
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    Pakku().context {
        terminal = Terminal(
            theme = Theme.Default,
            ansiLevel = AnsiLevel.TRUECOLOR,
            interactive = true
        )
    }.subcommands(
        Set(),
        Add(),
        Rm(),
        Ls(),
        Fetch()
    ).main(args)

    println("Program arguments: ${args.joinToString()}")
    exitProcess(1)
}

