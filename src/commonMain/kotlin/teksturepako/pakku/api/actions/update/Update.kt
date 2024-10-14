package teksturepako.pakku.api.actions.update

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import teksturepako.pakku.api.actions.ActionError
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
): Result<MutableSet<Project>, ActionError> = coroutineScope {
    val ghProjects = async {
        projects.filter { project ->
                GitHub.serialName in project.slug.keys
            }.map { oldProject ->
                val newProject = GitHub.requestProjectWithFiles(listOf(), listOf(), oldProject.slug[GitHub.serialName]!!)
                    ?.inheritPropertiesFrom(configFile) ?: return@map oldProject

                if (newProject.hasNoFiles()) oldProject else newProject // Do not update project if files are missing
            }
    }

    val combinedProjectsToOldFiles = projects.associateTo(mutableMapOf()) { it.copy(files = mutableSetOf()) to it.files }

    return@coroutineScope Ok(
        platforms.fold(combinedProjectsToOldFiles.keys.toMutableSet()) { acc, platform ->

            val listOfIds = projects.mapNotNull { it.id[platform.serialName] }

            platform.requestMultipleProjectsWithFiles(mcVersions, loaders, listOfIds, Int.MAX_VALUE)
                .inheritPropertiesFrom(configFile).forEach { newProject ->
                    acc.find { accProject ->
                        accProject.slug[platform.serialName] == newProject.slug[platform.serialName]
                    }?.also { accProject ->
                        val accFiles = combinedProjectsToOldFiles[accProject] ?: return@coroutineScope Err(
                            ActionError("Failed to combine project ${accProject.pakkuId} when updating." +
                                    " Report it to Pakku's developer.")
                        )

                        val accPublished = accFiles.find { it.type == platform.serialName }?.datePublished

                        if (accPublished != null && accPublished != Instant.DISTANT_FUTURE)
                        {
                            newProject.files.removeIf { it.type == platform.serialName && it.datePublished < accPublished }
                        }

                        // Combine projects
                        (accProject + newProject).get()
                            ?.copy(files = (newProject.files.take(numberOfFiles) + accProject.files).toMutableSet())
                            ?.let x@{ combinedProject ->
                                if (combinedProject.hasNoFiles()) return@x // Do not update project if files are missing

                                combinedProjectsToOldFiles[combinedProject] = accFiles
                                combinedProjectsToOldFiles -= accProject

                                acc -= accProject
                                acc += combinedProject
                            }
                    }
                }

            acc
        }.combineWith(ghProjects.await()).filter { newProject ->
            newProject !in projects && newProject.updateStrategy == UpdateStrategy.LATEST
        }.toMutableSet()
    )
}