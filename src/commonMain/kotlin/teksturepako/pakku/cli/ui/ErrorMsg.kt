package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.ActionError

fun Terminal.processErrorMsg(error: ActionError, arg: String = "", prepend: String? = null): String
{
    val msg = error.message(arg)
    val prep = if (prepend == null) "" else "$prepend "

    return if (error.isWarning)
    {
        this.theme.warning(prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>")))
    }
    else this.theme.danger(prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>")))
}