package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.typoSuggester

suspend fun Project?.createRemovalRequest(
    onWarning: suspend (message: String) -> Unit,
    onRemoval: suspend (project: Project, isRecommended: Boolean) -> Unit,
    onDepRemoval: suspend (project: Project, isRecommended: Boolean) -> Unit,
    projectArg: String,
    lockFile: LockFile,
)
{
    if (this != null)
    {
        val linkedProjects = lockFile.getLinkedProjects(this.pakkuId!!, ignore = this)

        if (linkedProjects.isEmpty())
        {
            onRemoval(this, true)
        }
        else
        {
            onWarning("$projectArg is required by ${linkedProjects.map { it.slug }}")
            onRemoval(this, false)
        }

        x@ for (pakkuLink in this.pakkuLinks)
        {
            val dependency = lockFile.getProjectByPakkuId(pakkuLink) ?: continue@x
            val depLinkedProjects = lockFile.getLinkedProjects(dependency.pakkuId!!)

            if (depLinkedProjects.isNotEmpty())
            {
                onWarning("${dependency.slug} is required by ${depLinkedProjects.map { it.slug }}")
                onDepRemoval(dependency, false)
            }
            else onDepRemoval(dependency, true)
        }
    }
    else
    {
        onWarning("$projectArg not found")
        val slugs = lockFile.getAllProjects().flatMap { it.slug.values }

        typoSuggester(projectArg, slugs).firstOrNull()?.let { arg ->
            onWarning("Did you mean $arg?")
        }
    }
}