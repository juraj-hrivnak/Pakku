package teksturepako.pakku.api.actions.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.update.combineProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.platforms.*
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.containNotProject
import teksturepako.pakku.api.projects.inheritPropertiesFrom
import teksturepako.pakku.debug
import teksturepako.pakku.debugIfNotEmpty
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readAndCreateSha1FromBytes
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.toPrettyString
import java.io.File
import kotlin.io.path.*

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
                path.toFile().walkTopDown().mapNotNull { file: File ->
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
        .debugIfNotEmpty {
            println(it.map { (path, _) -> path.pathString }.toPrettyString())
        }

    suspend fun List<Project>.withFoundSubpaths(): List<Project> = this.map { project ->
        val subpath = files.mapNotNull { (path, bytes) ->
            val projectFile = project.files
                .map { file ->
                    async {
                        val hash = file.hashes?.get("sha1")
                            ?: file.getPath(project, configFile).readAndCreateSha1FromBytes()
                        file to hash
                    }
                }
                .awaitAll()
                .find { (_, hash) ->
                    hash == createHash("sha1", bytes)
                }
                ?.first
                ?: return@mapNotNull null

            debug {
                println("Found subpath for ${projectFile.fileName}")
            }

            val subpath = path.absolute().invariantSeparatorsPathString
                .substringAfter(project.type.getPathString(configFile) + "/")
                .substringBefore(projectFile.fileName)
                .removeSuffix("/")

            subpath.ifBlank { return@mapNotNull null }
        }.firstOrNull()

        debug {
            println("Subpath: $subpath")
        }

        if (subpath != null)
        {
            project.copy().apply { setSubpath(subpath) }
        }
        else project
    }

    val cfProjectsDeferred = async {
        if (CurseForge in platforms)
        {
            CurseForge.requestMultipleProjectsWithFilesFromBytes(lockFile.getMcVersions(), files.map { it.second })
                .inheritPropertiesFrom(configFile)
        }
        else mutableSetOf()
    }

    val mrProjectsDeferred = async {
        if (Modrinth in platforms)
        {
            Modrinth.requestMultipleProjectsWithFilesFromHashes(files.map { createHash("sha1", it.second) }, "sha1")
                .inheritPropertiesFrom(configFile)
        }
        else mutableSetOf()
    }

    val deferredPlatformsToProjects = async {
        listOf(CurseForge to cfProjectsDeferred.await(), Modrinth to mrProjectsDeferred.await())
    }

    val detectedProjects = deferredPlatformsToProjects.await()
        .fold(deferredPlatformsToProjects.await().flatMap { it.second }) { accProjects, (platform, platformProjects) ->
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

    val addedProjects = detectedProjects
        .filter { detectedProject -> currentProjects containNotProject detectedProject }
        .withFoundSubpaths()
        .toSet()

    val removedProjects = currentProjects
        .filter { currentProject -> detectedProjects containNotProject currentProject }
        .withFoundSubpaths()
        .toSet()

    val updatedProjects = Multiplatform.platforms.fold(currentProjects) { accProjects, platform ->
        accProjects.map { accProject ->
            detectedProjects.find { it.slug[platform.serialName] == accProject.slug[platform.serialName] }
                ?.let { newProject -> combineProjects(accProject, newProject, platform.serialName, 1) }
                ?: accProject
        }
    }
        .distinctBy { it.files }
        .withFoundSubpaths()
        .filter { project -> project !in currentProjects }
        .toSet()

    return@coroutineScope SyncResult(addedProjects, removedProjects, updatedProjects)
}
