package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.success
import teksturepako.pakku.api.actions.errors.ActionError
import kotlin.time.Duration

fun dim(text: Any?): String = TextStyle(color = TextColors.gray)(text.toString())
fun String.plusDim(string: String): String = this + TextStyle(color = TextColors.gray)(string)

fun strong(string: String): String = TextStyle(bold = true, underline = true)(string)
fun strong(text: Any?): String = TextStyle(bold = true, underline = true)(text.toString())
fun String.plusStrong(string: String): String = this + TextStyle(bold = true, underline = true)(string)

fun String.createHyperlink(hyperlink: String): String = TextStyle(hyperlink = hyperlink)(this)

fun <T : Any> Collection<T>.toMsg(): String
{
    if (this.size == 1) return this.first().toString()

    return this.foldIndexed("") { i, acc, value ->
        when
        {
            acc.isBlank()      -> acc + value.toString()
            this.size <= i + 1 -> "$acc and $value"
            else               -> "$acc, $value"
        }
    }
}

fun Terminal.pSuccess(message: String, offset: Int = 0)
{
    this.success(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>"), offset))
}

fun Terminal.pError(error: ActionError, arg: String = "", prepend: String? = null, offset: Int = 0)
{
    this.println(processErrorMsg(error, arg, prepend, offset))
}

fun Terminal.pInfo(message: String, offset: Int = 0)
{
    this.info(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>"), offset))
}

fun Terminal.pDanger(message: String, offset: Int = 0)
{
    this.danger(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>"), offset))
}

fun Terminal.pMsg(message: String, offset: Int = 0)
{
    this.println(prefixed(message, prefix = this.theme.string("pakku.prefix", ">>>"), offset))
}

fun prefixed(text: String, prefix: String, offset: Int = 0): String = buildString {
    repeat(offset) { append(" ".repeat(3)) }
    append("${TextStyle(inverse = true)(prefix)} $text")
}

fun offset(text: String, offset: Int): String = buildString {
    repeat(offset) { append(" ".repeat(3)) }
    append(text)
}

fun Duration.shortForm() = this.toString().replace("\\.\\d+".toRegex(), "")
