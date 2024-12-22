package teksturepako.pakku.api.actions.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.update.combineProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.containNotProject
import teksturepako.pakku.api.projects.inheritPropertiesFrom
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakku.io.tryOrNull
import java.io.File
import kotlin.io.path.*

data class SyncResult(
    val added: Set<Project>,
    val removed: Set<Project>,
)

suspend fun syncProjects(
    lockFile: LockFile,
    configFile: ConfigFile?,
    platforms: List<Platform>,
): SyncResult = coroutineScope {

    val defaultIgnoredPaths = listOf("saves", "screenshots")
    val allowedExtensions = listOf(".jar", ".zip")

    val files = ProjectType.entries
        .filterNot { it == ProjectType.WORLD }
        .mapNotNull { projectType ->
            val prjTypeDir = Path(workingPath, projectType.getPathString(configFile))
            if (prjTypeDir.notExists() || defaultIgnoredPaths.any { it in prjTypeDir.pathString }) return@mapNotNull null

            prjTypeDir
        }
        .mapNotNull { dir ->
            dir.tryOrNull { path ->
                path.toFile().walkBottomUp().mapNotNull { file: File ->
                    file.toPath().takeIf { it != dir }
                }
            }
        }
        .flatMap { pathSequence ->
            pathSequence.toSet().map { path ->
                async {
                    if (path.isDirectory() || allowedExtensions.none { path.pathString.endsWith(it) }) return@async null

                    val bytes = readPathBytesOrNull(path) ?: return@async null

                    path to bytes
                }
            }
        }
        .awaitAll()
        .filterNotNull()


    fun MutableList<Project>.withFoundSubpath(): List<Project> = this.flatMap { project ->
        files.map x@ { (path, _) ->
            val fileName = project.files.firstOrNull()?.fileName ?: return@x project

            if (!path.pathString.endsWith(fileName)) return@x project

            val subpath = path.absolute().invariantSeparatorsPathString
                .substringAfter(project.type.getPathString(configFile) + "/")
                .substringBefore(fileName)

            if (subpath.isBlank()) return@x project

            project.setSubpath(subpath)
            project
        }
    }

    val cfProjectsDeferred = async {
        if (CurseForge in platforms)
        {
            val projects = CurseForge.requestMultipleProjectsWithFilesFromBytes(lockFile.getMcVersions(), files.map { it.second })
                .inheritPropertiesFrom(configFile)

            projects.withFoundSubpath()
        }
        else mutableSetOf()
    }

    val mrProjectsDeferred = async {
        if (Modrinth in platforms)
        {
            val projects = Modrinth.requestMultipleProjectsWithFilesFromHashes(files.map { createHash("sha1", it.second) }, "sha1")
                .inheritPropertiesFrom(configFile)

            projects.withFoundSubpath()
        }
        else mutableSetOf()
    }

    val detectedProjects = async {
        listOf(CurseForge to cfProjectsDeferred.await(), Modrinth to mrProjectsDeferred.await())
    }

    val combinedProjects = detectedProjects.await()
        .fold(detectedProjects.await().flatMap { it.second }) { accProjects, (platform, platformProjects) ->
            accProjects.map { accProject ->
                platformProjects.find { it isAlmostTheSameAs accProject }
                    ?.let { newProject -> combineProjects(accProject, newProject, platform.serialName, 1) }
                    ?: accProject
            }
        }
        .distinctBy { it.files }
        .sortedBy { it.name.values.firstOrNull() }

    val currentProjects = lockFile.getAllProjects().filterNot {
        it.slug.keys.firstOrNull() == GitHub.serialName && it.slug.keys.size == 1
    }

    val addedProjects = combinedProjects
        .filter { detectedProject -> currentProjects containNotProject detectedProject }
        .toSet()

    val removedProjects = currentProjects
        .filter { currentProject -> combinedProjects containNotProject currentProject }
        .toSet()

    return@coroutineScope SyncResult(addedProjects, removedProjects)
}
