package teksturepako.pakku.api.actions

import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.ProjNotFound
import teksturepako.pakku.api.actions.errors.ProjRequiredBy
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project

suspend fun Project?.createRemovalRequest(
    onError: suspend (error: ActionError) -> Unit,
    onRemoval: suspend (project: Project, isRecommended: Boolean) -> Unit,
    onDepRemoval: suspend (project: Project, isRecommended: Boolean) -> Unit,
    lockFile: LockFile,
)
{
    if (this == null) return onError(ProjNotFound())

    val dependants = lockFile.getLinkedProjects(this.pakkuId!!, ignore = this)

    if (dependants.isEmpty())
    {
        onRemoval(this, true)
    }
    else
    {
        onError(ProjRequiredBy(this, dependants))
        onRemoval(this, false)
    }

    for (pakkuLink in this.pakkuLinks)
    {
        val dependency = lockFile.getProjectByPakkuId(pakkuLink) ?: continue
        val depDependants = lockFile.getLinkedProjects(dependency.pakkuId!!)

        if (depDependants.isNotEmpty())
        {
            onError(ProjRequiredBy(dependency, depDependants))
            onDepRemoval(dependency, false)
        }
        else onDepRemoval(dependency, true)
    }
}