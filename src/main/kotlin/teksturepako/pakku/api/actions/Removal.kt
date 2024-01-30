package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.typoSuggester

suspend fun Project?.createRemovalRequest(
    onWarning: WarningBlock,
    onRemoval: RemovalBlock,
    onDepRemoval: RemovalBlock,
    projectArg: String,
    pakkuLock: PakkuLock,
)
{
    if (this != null)
    {
        val linkedProjects = pakkuLock.getLinkedProjects(this.pakkuId!!, ignore = this)

        if (linkedProjects.isEmpty())
        {
            onRemoval.remove(this, true)
        }
        else
        {
            onWarning.warning("$projectArg is required by ${linkedProjects.map { it.slug }}")
            onRemoval.remove(this, false)
        }

        x@ for (pakkuLink in this.pakkuLinks)
        {
            val dependency = pakkuLock.getProjectByPakkuId(pakkuLink) ?: continue@x
            val depLinkedProjects = pakkuLock.getLinkedProjects(dependency.pakkuId!!)

            if (depLinkedProjects.isNotEmpty())
            {
                onWarning.warning("${dependency.slug} is required by ${depLinkedProjects.map { it.slug }}")
                onDepRemoval.remove(dependency, false)
            }
            else onDepRemoval.remove(dependency, true)
        }
    }
    else
    {
        onWarning.warning("$projectArg not found")
        val slugs = pakkuLock.getAllProjects().flatMap { it.slug.values }

        typoSuggester(projectArg, slugs).firstOrNull()?.let { arg ->
            onWarning.warning("Did you mean $arg?")
        }
    }
}