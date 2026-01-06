package teksturepako.pakku.api.actions.update

import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.inheritPropertiesFrom
import teksturepako.pakku.io.mapAsync

/**
 * Requests new data for provided [projects] from all platforms and updates them based on platform-specific slugs,
 * with optional [number of files][numberOfFiles] to take.
 * Projects are also filtered using their [update strategy][UpdateStrategy].
 */
suspend fun updateMultipleProjectsWithFiles(
    onError: suspend (error: ActionError) -> Unit,
    mcVersions: List<String>,
    loaders: List<String>,
    projects: MutableSet<Project>,
    configFile: ConfigFile?,
    platforms: List<Platform>,
    numberOfFiles: Int,
): MutableSet<Project> = coroutineScope {
    val ghProjectsDeferred = async {
        projects
            .filter { GitHub.serialName in it.slug.keys }
            .mapAsync { oldProject ->
                val ghSlug = oldProject.slug[GitHub.serialName] ?: return@mapAsync oldProject
                GitHub.requestProjectWithFiles(emptyList(), emptyList(), ghSlug, projectType = oldProject.type)
                    .onFailure { onError(it) }
                    .get()
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
        ).onFailure { onError(it) }.get()?.inheritPropertiesFrom(configFile)

        accProjects.map { accProject ->
            platformProjects?.find { it.slug[platform.serialName] == accProject.slug[platform.serialName] }
                ?.let { newProject -> combineProjects(accProject, newProject, platform.serialName, numberOfFiles, mcVersions) }
                ?: accProject
        }.toMutableSet()
    }

    val ghProjects = ghProjectsDeferred.await().toSet()

    updatedProjects.map { accProject ->
        ghProjects.find { it.slug[GitHub.serialName] == accProject.slug[GitHub.serialName] }
            ?.let { newProject -> combineProjects(accProject, newProject, GitHub.serialName, numberOfFiles) }
            ?: accProject
    }
        .filter { it.updateStrategy == UpdateStrategy.LATEST && it !in projects }
        .toMutableSet()
}

fun combineProjects(
    accProject: Project, newProject: Project, platformName: String, numberOfFiles: Int, mcVersions: List<String> = listOf(),
): Project
{
    val accLoaders = accProject.files
        .filter { it.type == platformName }
        .maxByOrNull { it.datePublished }
        ?.loaders
        ?: listOf()

    val newFiles = newProject.files
        .filter { it.type == platformName }
        .sortedWith(
            compareBy<ProjectFile> { file ->
                // prefer mc version higher in the lock file
                mcVersions.indexOfFirst { it in file.mcVersions }
                    .let { if (it == -1) mcVersions.size else it }
            }.thenBy { file ->
                // prefer the current loader
                accLoaders.indexOfFirst { it in file.loaders }
                    .let { if (it == -1) accLoaders.size else it }
            }.thenByDescending {
                // prefer newer file
                it.datePublished
            }
        )

    val updatedFiles = (newFiles.take(numberOfFiles) + accProject.files.filter { it.type != platformName })
        .distinctBy { it.type }
        .toMutableSet()

    return (accProject + newProject).get()?.copy(files = updatedFiles)
        ?.takeIf { it.hasFiles() } ?: accProject
}
