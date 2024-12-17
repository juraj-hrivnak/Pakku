package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ErrorSeverity

fun Terminal.processErrorMsg(error: ActionError, arg: String = "", prepend: String? = null, offset: Int = 0): String
{
    val msg = error.message(arg)
    val prep = if (prepend == null) "" else "$prepend "

    return when (error.severity)
    {
        ErrorSeverity.ERROR ->
        {
            this.theme.danger(prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>"), offset))
        }
        ErrorSeverity.WARNING ->
        {
            this.theme.warning(prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>"), offset))
        }
        ErrorSeverity.NOTICE  ->
        {
            prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>"), offset)
        }
    }
}