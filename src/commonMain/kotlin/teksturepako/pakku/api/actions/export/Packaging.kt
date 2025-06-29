package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.actions.errors.ActionError
import java.nio.file.Path

/** Packaging class is used to store the action of a [rule result][RuleResult]. */
sealed class Packaging
{
    data class Error(val error: ActionError) : Packaging()
    data object Ignore : Packaging()
    data object EmptyAction : Packaging()
    data class Action(val action: suspend () -> ActionError?) : Packaging()
    data class FileAction(val action: suspend () -> Pair<Path, ActionError?>) : Packaging()
}
