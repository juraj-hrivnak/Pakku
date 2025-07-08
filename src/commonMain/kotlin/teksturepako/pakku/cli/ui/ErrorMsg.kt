package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ErrorSeverity
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.cli.arg.promptForCurseForgeApiKey

fun Terminal.processErrorMsg(
    msg: String,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    prepend: String? = null,
    offset: Int = 0
): String
{
    val prep = if (prepend == null) "" else "$prepend "

    return when (severity)
    {
        ErrorSeverity.FATAL ->
        {
            this.theme.danger(prefixed("$prep$msg", this.theme.string("pakku.prefix", ">>>"), offset))
        }
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

fun Terminal.processErrorMsg(error: ActionError, arg: String = "", prepend: String? = null, offset: Int = 0): String
{
    val msg = error.message(arg)

    return processErrorMsg(msg, error.severity, prepend, offset)
}

fun Terminal.processShortErrorMsg(error: ActionError, arg: String = "", prepend: String? = null, offset: Int = 0): String
{
    val msg = error.shortMessage(arg) ?: return ""

    return processErrorMsg(msg, error.severity, prepend, offset)
}

suspend fun Terminal.pErrorOrPrompt(error: ActionError)
{
    if (error is CurseForge.Unauthenticated)
    {
        this.pError(error)
        this.promptForCurseForgeApiKey()?.onError { this.pError(it) }
        this.println()
    }
    else
    {
        this.pError(error)
        this.println()
    }
}