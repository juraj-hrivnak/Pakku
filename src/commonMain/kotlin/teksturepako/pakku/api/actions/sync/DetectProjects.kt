package teksturepako.pakku.api.actions.sync

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.actions.update.combineProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.inheritPropertiesFrom
import teksturepako.pakku.debugIfNotEmpty
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readPathBytesOrNull
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.toPrettyString
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.pathString

suspend fun detectProjects(
    lockFile: LockFile,
    configFile: ConfigFile?,
    platforms: List<Platform>,
): Set<Project> = coroutineScope {

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

                    Triple(path, bytes, createHash("sha1", bytes))
                }
            }
        }
        .awaitAll()
        .filterNotNull()
        .debugIfNotEmpty {
            println(it.map { (path, _) -> path.pathString }.toPrettyString())
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
            Modrinth.requestMultipleProjectsWithFilesFromHashes(files.map { it.third }, "sha1")
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
        .toSet()

    return@coroutineScope detectedProjects
}
