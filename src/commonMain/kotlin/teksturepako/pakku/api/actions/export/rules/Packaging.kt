package teksturepako.pakku.api.actions.export.rules

import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError

sealed class Packaging
{
    data object Ignore : Packaging()
    data object EmptyAction : Packaging()
    data class Action(val action: suspend () -> Result<Any?, ActionError>) : Packaging()
}