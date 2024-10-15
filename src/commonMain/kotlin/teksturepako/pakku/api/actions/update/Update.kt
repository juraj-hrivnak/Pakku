package teksturepako.pakku.api.actions.update

import com.github.michaelbull.result.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
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
    val ghProjectsDeferred = async {
        projects
            .filter { GitHub.serialName in it.slug.keys }
            .map { oldProject ->
                val ghSlug = oldProject.slug[GitHub.serialName] ?: return@map oldProject
                GitHub.requestProjectWithFiles(emptyList(), emptyList(), ghSlug)
                    ?.inheritPropertiesFrom(configFile)
                    ?.takeIf { it.hasFiles() }
                    ?: oldProject
            }
    }

    val updatedProjects = platforms.fold(projects) { accProjects, platform ->
        val platformProjects = platform.requestMultipleProjectsWithFiles(
            mcVersions,
            loaders,
            accProjects.mapNotNull { it.id[platform.serialName] },
            Int.MAX_VALUE
        ).inheritPropertiesFrom(configFile)

        accProjects.map { accProject ->
            platformProjects.find { it.slug[platform.serialName] == accProject.slug[platform.serialName] }
                ?.let { newProject -> combineProjects(accProject, newProject, platform.serialName, numberOfFiles) }
                ?: accProject
        }.toMutableSet()
    }

    val ghProjects = ghProjectsDeferred.await().toSet()

    (updatedProjects combineWith ghProjects)
        .filter { it.updateStrategy == UpdateStrategy.LATEST && it !in projects }
        .toMutableSet()
}

private fun combineProjects(accProject: Project, newProject: Project, platformName: String, numberOfFiles: Int): Project
{
    val accPublished = accProject.files
        .filter { projectFile ->
            projectFile.type == platformName
        }
        .maxOfOrNull { it.datePublished }
        ?: Instant.DISTANT_PAST

    val updatedFiles = (newProject.files.take(numberOfFiles) + accProject.files)
        .filterNot { projectFile ->
            projectFile.type == platformName && projectFile.datePublished < accPublished
        }
        .toMutableSet()

    return (accProject + newProject).get()?.copy(files = updatedFiles)
        ?.takeIf { it.hasFiles() } ?: accProject
}