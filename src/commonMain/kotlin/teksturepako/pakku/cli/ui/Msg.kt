package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.ActionError
import kotlin.time.Duration

fun dim(string: String): String = TextStyle(color = TextColors.gray)(string)
fun String.addDim(string: String): String = this + TextStyle(color = TextColors.gray)(string)

fun strong(string: String): String = TextStyle(bold = true, underline = true)(string)
fun String.addStrong(string: String): String = this + TextStyle(bold = true, underline = true)(string)

fun String.createHyperlink(hyperlink: String): String = TextStyle(hyperlink = hyperlink)(this)


fun Terminal.pSuccess(message: String)
{
    this.success(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>")))
}

fun Terminal.pError(error: ActionError, arg: String = "", prepend: String? = null)
{
    this.println(processErrorMsg(error, arg, prepend))
}

fun Terminal.pInfo(message: String)
{
    this.info(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>")))
}

fun Terminal.pDanger(message: String)
{
    this.danger(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>")))
}

fun prefixed(string: String, prefix: String, offset: Int = 0): String = buildString {
    repeat(offset) { append(" ".repeat(3)) }
    append ("$prefix $string")
}

fun Duration.shortForm() = this.toString().replace("\\.\\d+".toRegex(), "")