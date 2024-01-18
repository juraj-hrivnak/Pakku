package teksturepako.pakku.api.projects

import teksturepako.pakku.api.platforms.Platform

fun Collection<Project>.assignFiles(files: Collection<ProjectFile>, platform: Platform)
{
    this.forEach { project ->
        files.forEach { file ->
            if (project.id[platform.serialName] == file.parentId)
            {
                project.files.add(file)
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
            project + it
        } ?: project
    }.toSet()
}

