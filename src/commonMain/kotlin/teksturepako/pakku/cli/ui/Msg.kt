package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import kotlin.time.Duration

fun dim(string: String): String = TextStyle(color = TextColors.gray)(string)
fun String.addDim(string: String): String = this + TextStyle(color = TextColors.gray)(string)

fun strong(string: String): String = TextStyle(bold = true, underline = true)(string)
fun String.addStrong(string: String): String = this + TextStyle(bold = true, underline = true)(string)

fun String.createHyperlink(hyperlink: String): String = TextStyle(hyperlink = hyperlink)(this)


private const val PREFIX_LENGTH = 3

val prefix = TextStyle(inverse = true)("❯❯❯") + " "

fun prefixed(string: String, offset: Int = 0): String = buildString {
    repeat(offset) { append(" ".repeat(PREFIX_LENGTH)) }
    append (prefix + string)
}

fun Duration.shortForm() = this.toString().replace("\\.\\d+".toRegex(), "")