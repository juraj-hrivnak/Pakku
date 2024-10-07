package teksturepako.pakku.api.actions.update

import com.github.michaelbull.result.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Multiplatform.platforms
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.api.projects.inheritPropertiesFrom

/**
 * Requests new data for provided [projects] from all platforms and updates them based on platform-specific slugs,
 * with optional [number of files][numberOfFiles] to take.
 * Projects are also filtered using their [update strategy][UpdateStrategy].
 */
suspend fun updateMultipleProjectsWithFiles(
    mcVersions: List<String>,
    loaders: List<String>,
    projects: MutableSet<Project>,
    configFile: ConfigFile?,
    numberOfFiles: Int
): MutableSet<Project> = coroutineScope {
    val ghProjects = async {
        projects.filter { project ->
                GitHub.serialName in project.slug.keys
            }.map { oldProject ->
                val newProject = GitHub.requestProjectWithFiles(listOf(), listOf(), oldProject.slug[GitHub.serialName]!!)
                    ?.inheritPropertiesFrom(configFile) ?: return@map oldProject

                if (newProject.hasNoFiles()) oldProject else newProject // Do not update project if files are missing
            }
    }

    return@coroutineScope platforms.fold(projects) { acc, platform ->

        val listOfIds = projects.mapNotNull { it.id[platform.serialName] }

        platform.requestMultipleProjectsWithFiles(mcVersions, loaders, listOfIds, Int.MAX_VALUE)
            .inheritPropertiesFrom(configFile).forEach { newProject ->
                acc.find { accProject ->
                    accProject.slug[platform.serialName] == newProject.slug[platform.serialName]
                }?.also { accProject ->
                    // Combine projects
                    newProject.files.removeIf { newFile -> newFile.type == platform.serialName && newFile.dataPublished >= accProject.files.find { it.type == platform.serialName }?.dataPublished }
                    (accProject + newProject).get()?.copy(files = newProject.files.take(numberOfFiles).toMutableSet())
                        ?.let x@{ combinedProject ->
                            acc -= accProject
                            if (combinedProject.hasNoFiles())
                            {
                                acc += accProject.copy(files = mutableSetOf())
                                return@x // Do not update project if files are missing
                            }
                            acc += combinedProject
                        }
                }
            }

        acc
    }.combineWith(ghProjects.await()).filter { newProject ->
        projects.none { oldProject ->
            oldProject == newProject
        } && newProject.updateStrategy == UpdateStrategy.LATEST
    }.toMutableSet()
}