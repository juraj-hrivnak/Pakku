package teksturepako.pakku.api.projects

import com.github.michaelbull.result.get
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.platforms.Platform

fun Collection<Project>.inheritPropertiesFrom(configFile: ConfigFile?): MutableList<Project>
{
    if (configFile == null) return this.toMutableList()

    configFile.getProjects().forEach { (input, config) ->
        this.forEach { project ->
            if (input in project || project.files.any { input in it.fileName })
            {
                project.side = config.side
                config.updateStrategy?.let { project.updateStrategy = it }
                config.redistributable?.let { project.redistributable = it }
            }
        }
    }

    return this.toMutableList()
}

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

fun MutableCollection<Project>.removeIf(predicate: (Project) -> Boolean): Boolean
{
    return this.removeAll(this.filter { predicate(it) }.toSet())
}

/** Combines (or zips) projects with other projects. */
fun Collection<Project>.combineWith(otherProjects: Collection<Project>): Set<Project>
{
    return this.map { project ->
        otherProjects.find { project isAlmostTheSameAs it }?.let {
            if (project.type == it.type) (project + it).get() else project
        } ?: project
    }.toSet()
}

