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
                GitHub.requestProjectWithFiles(emptyList(), emptyList(), ghSlug, projectType = oldProject.type)
                    ?.inheritPropertiesFrom(configFile)
                    ?.takeIf { it.hasFiles() }
                    ?: oldProject
            }
    }

    val updatedProjects = platforms.fold(projects) { accProjects, platform ->
        val platformProjects = platform.requestMultipleProjectsWithFiles(
            mcVersions,
            loaders,
            accProjects.mapNotNull { project -> project.id[platform.serialName]?.let { it to project.type } }.toMap(),
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
    val accFile = accProject.files.filter { projectFile ->
        projectFile.type == platformName
    }.maxByOrNull { it.datePublished }

    val accPublished = accFile?.datePublished ?: Instant.DISTANT_PAST

    val newFiles = if (accFile == null) newProject.files else
    {
        newProject.files.sortedWith(compareBy { file ->
            accFile.loaders.indexOfFirst { it in file.loaders }.let { if (it == -1) accFile.loaders.size else it }
        })
    }

    val updatedFiles = (newFiles.take(numberOfFiles) + accProject.files).filterNot { projectFile ->
            projectFile.type == platformName && projectFile.datePublished < accPublished
        }
        .distinctBy { it.type }
        .toMutableSet()

    return (accProject + newProject).get()?.copy(files = updatedFiles)
        ?.takeIf { it.hasFiles() } ?: accProject
}
