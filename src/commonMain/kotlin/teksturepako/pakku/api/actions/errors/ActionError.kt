package teksturepako.pakku.api.actions.errors

abstract class ActionError
{
    abstract val rawMessage: String

    open val severity: ErrorSeverity = ErrorSeverity.ERROR

    /** A message dedicated for the CLI. It should not be used outside of terminal. */
    open fun message(arg: String = ""): String = rawMessage

    protected fun message(vararg args: String): String = args.joinToString("")
}
