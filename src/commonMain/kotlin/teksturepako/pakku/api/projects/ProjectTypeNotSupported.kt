package teksturepako.pakku.api.projects

import teksturepako.pakku.api.actions.errors.ActionError

class ProjectTypeNotSupported(slug: String, projectType: String) : ActionError()
{
    override val rawMessage = "$slug: Project type $projectType from CurseForge isn't supported"
}
