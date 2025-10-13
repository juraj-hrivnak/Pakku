package teksturepako.pakku.api.actions.sync

import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.containNotProject

data class SyncResult(
    val added: Set<Project>,
    val removed: Set<Project>,
    val updated: Set<Project>,
)

suspend fun syncProjects(
    onError: suspend (error: ActionError) -> Unit,
    lockFile: LockFile,
    configFile: ConfigFile?,
    platforms: List<Platform>,
): SyncResult = coroutineScope {

    val detectedProjects = detectProjects(onError, lockFile, configFile, platforms)

    val currentProjects = lockFile.getAllProjects().filterNot {
        it.slug.keys.firstOrNull() == GitHub.serialName && it.slug.keys.size == 1
    }.toSet()

    val addedProjects = detectedProjects
        .filter { detectedProject -> currentProjects containNotProject detectedProject }
        .toSet()

    val removedProjects = currentProjects
        .filter { currentProject -> detectedProjects containNotProject currentProject }
        .toSet()

    val updatedProjects = (detectedProjects - addedProjects - removedProjects)
        .distinctBy { it.files }
        .filter { detectedProject ->
            currentProjects
                .find { currentProject ->
                    currentProject.slug.any { (platform, slug) -> detectedProject.slug[platform] == slug }
                }?.let { currentProject ->
                    // Only consider updated if files are different
                    currentProject.files != detectedProject.files
                }
                ?: false // Fallback to false if not project is found
        }
        .toSet()

    return@coroutineScope SyncResult(addedProjects, removedProjects, updatedProjects)
}
