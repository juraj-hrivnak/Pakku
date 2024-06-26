package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.Theme
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*

fun processErrorMsg(error: ActionError, arg: String = "", prefix: String = ""): String
{
    val msg = when (error)
    {
        // -- PROJECT --

        is ProjNotFound       -> "Project '$arg' not found."

        // -- ADDITION --

        is AlreadyAdded       -> "Could not add ${error.project.getFlavoredSlug()}. It is already added."
        is NotFoundOnPlatform -> "${error.project.getFlavoredSlug()} was not found on ${error.platform.name}."

        // -- REMOVAL --

        is ProjRequiredBy     -> "${error.project.getFlavoredSlug()} is required by " +
                "${error.dependants.map { it.getFlavoredSlug() }}"

        else                  -> error.message
    }

    return if (error.isWarning)
    {
        Theme.Default.warning(prefixed("$prefix $msg"))
    }
    else Theme.Default.danger(prefixed("$prefix $msg"))
}