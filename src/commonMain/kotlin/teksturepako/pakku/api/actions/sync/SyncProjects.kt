package teksturepako.pakku.api.actions.sync

import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.update.combineProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.containNotProject

data class SyncResult(
    val added: Set<Project>,
    val removed: Set<Project>,
    val updated: Set<Project>,
)

suspend fun syncProjects(
    lockFile: LockFile,
    configFile: ConfigFile?,
    platforms: List<Platform>,
): SyncResult = coroutineScope {

    val detectedProjects = detectProjects(lockFile, configFile, platforms)

    val currentProjects = lockFile.getAllProjects().filterNot {
        it.slug.keys.firstOrNull() == GitHub.serialName && it.slug.keys.size == 1
    }

    val addedProjects = detectedProjects
        .filter { detectedProject -> currentProjects containNotProject detectedProject }
        .toSet()

    val removedProjects = currentProjects
        .filter { currentProject -> detectedProjects containNotProject currentProject }
        .toSet()

    val updatedProjects = Multiplatform.platforms.fold(currentProjects) { accProjects, platform ->
        accProjects.map { accProject ->
            detectedProjects.find { it.slug[platform.serialName] == accProject.slug[platform.serialName] }
                ?.let { newProject -> combineProjects(accProject, newProject, platform.serialName, 1) }
                ?: accProject
        }
    }
        .distinctBy { it.files }
        .filter { project -> project !in currentProjects }
        .toSet()

    return@coroutineScope SyncResult(addedProjects, removedProjects, updatedProjects)
}
