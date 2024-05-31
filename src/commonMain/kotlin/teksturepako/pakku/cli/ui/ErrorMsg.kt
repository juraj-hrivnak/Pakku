package teksturepako.pakku.cli.ui

import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*

fun processErrorMsg(error: ActionError, arg: String = ""): String
{
    val msg = when (error)
    {
        // -- ADDITION --

        is ProjNotFound       -> "Project '$arg' not found."
        is AlreadyAdded       -> "Could not add ${error.project.getFlavoredSlug()}. It is already added."
        is NotFoundOnPlatform -> "${error.project.getFlavoredSlug()} was not found on ${error.platform.name}."
        else                  -> error.message
    }

    return prefixed(msg)
}