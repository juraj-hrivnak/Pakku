package teksturepako

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.cmd.Comp
import teksturepako.cmd.Get
import teksturepako.cmd.Pakku
import kotlin.system.exitProcess

fun main(args: Array<String>)
{
    println()

    Pakku().context {
        terminal = Terminal(
            ansiLevel = AnsiLevel.TRUECOLOR,
            interactive = true
        )
    }.subcommands(
        Comp(),
        Get()
    ).main(args)

    println("Program arguments: ${args.joinToString()}")
    exitProcess(1)
}

