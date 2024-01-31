package teksturepako.pakku.api.projects

import teksturepako.pakku.api.platforms.Platform

fun Collection<Project>.assignFiles(projectFiles: Collection<ProjectFile>, platform: Platform)
{
    this.forEach { project ->
        projectFiles.forEach { projectFile ->
            if (project.id[platform.serialName] == projectFile.parentId)
            {
                project.files.add(projectFile)
            }
        }
    }
}

infix fun Collection<Project>.containsProject(project: Project): Boolean =
    this.any { it isAlmostTheSameAs project }

infix fun Collection<Project>.containsNotProject(project: Project): Boolean =
    !this.containsProject(project)

fun Collection<Project>.combineWith(otherProjects: Collection<Project>): Set<Project>
{
    return this.map { project ->
        otherProjects.find { project isAlmostTheSameAs it }?.let {
            if (project.type == it.type) project + it else project
        } ?: project
    }.toSet()
}

