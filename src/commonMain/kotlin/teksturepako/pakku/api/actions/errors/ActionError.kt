package teksturepako.pakku.api.actions.errors

import teksturepako.pakku.cli.ui.offset

abstract class ActionError
{
    abstract val rawMessage: String

    open val severity: ErrorSeverity = ErrorSeverity.ERROR

    /** A message dedicated for the CLI. It should not be used outside of terminal. */
    open fun message(arg: String = ""): String = rawMessage

    protected fun message(vararg args: Any?, newlines: Boolean = false, offset: Int = 0): String
    {
        val text = if (newlines)
        {
            args.joinToString("\n") { it?.toString() ?: "" }
        }
        else
        {
            args.joinToString("") { arg ->
                when (arg)
                {
                    null      -> ""
                    is String -> arg
                    else      -> "$arg "
                }
            }
        }

        return text.split("\n").joinToString("\n") { offset(it, offset) }
    }

    protected fun optionalArg(arg: Any?): String = if (arg != null) " '$arg'" else ""

    inline fun <T> onError(action: (ActionError) -> T): T = action(this)
}

class MultipleErrors(vararg val errors: ActionError) : ActionError()
{
    override val rawMessage: String = errors.joinToString("\n") { it.rawMessage }
    override fun message(arg: String): String = errors.joinToString("\n") { it.message(arg) }
}

fun Collection<ActionError?>.toMultipleErrors(): MultipleErrors? = this
    .filterNotNull()
    .ifEmpty { null }
    ?.toTypedArray()
    ?.let { MultipleErrors(*it) }
