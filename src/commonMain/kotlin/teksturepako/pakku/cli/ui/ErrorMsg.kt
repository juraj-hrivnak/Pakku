package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.Theme
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*

fun processErrorMsg(error: ActionError, arg: String = "", prepend: String? = null): String
{
    val msg = error.message(arg)
    val prep = if (prepend == null) "" else "$prepend "

    return if (error.isWarning)
    {
        Theme.Default.warning(prefixed("$prep$msg"))
    }
    else Theme.Default.danger(prefixed("$prep$msg"))
}