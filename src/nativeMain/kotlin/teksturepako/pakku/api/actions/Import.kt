package teksturepako.pakku.api.actions

import korlibs.io.async.async
import korlibs.io.file.baseName
import korlibs.io.file.extension
import korlibs.io.file.pathInfo
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.models.CfModpackModel
import teksturepako.pakku.api.models.MrModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.assignFiles
import teksturepako.pakku.api.projects.combineWith
import teksturepako.pakku.debug
import teksturepako.pakku.io.readFileOrNull
import teksturepako.pakku.io.unzip

const val CF_MANIFEST = "manifest.json"
const val MR_MANIFEST = "modrinth.index.json"

suspend fun import(
    onError: ErrorBlock,
    path: String,
    pakkuLock: PakkuLock,
    platforms: List<Platform>
): Set<Project> = when
{
    path.endsWith("zip") || CF_MANIFEST in path    ->
    {
        importCurseForge(path).asSetOfCfProjects(onError, pakkuLock, platforms)
    }
    path.endsWith("mrpack") || MR_MANIFEST in path ->
    {
        importModrinth(path).asSetOfMrProjects(onError, pakkuLock, platforms)
    }
    else -> setOf()
}

suspend fun importCfManifestFile(path: String): CfModpackModel? =
    if (path.pathInfo.baseName == CF_MANIFEST)
    {
        readFileOrNull(path)?.let { json.decodeFromString<CfModpackModel>(it) }
    }
    else null

suspend fun importCfPackFile(path: String): CfModpackModel? =
    runCatching { unzip(path)[CF_MANIFEST].readString() }.getOrNull()?.let {
        json.decodeFromString<CfModpackModel>(it)
    }

suspend fun importCurseForge(path: String): Pair<CfModpackModel?, String> =
    if (path.pathInfo.extension == "zip")
    {
        importCfPackFile(path) to path
    }
    else importCfManifestFile(path) to path


suspend fun importMrManifestFile(path: String): MrModpackModel? =
    if (path.pathInfo.baseName == MR_MANIFEST)
    {
        readFileOrNull(path)?.let { json.decodeFromString<MrModpackModel>(it) }
    }
    else null


suspend fun importMrPackFile(path: String): MrModpackModel? =
    runCatching { unzip(path)[MR_MANIFEST].readString() }.getOrNull()?.let {
        json.decodeFromString<MrModpackModel>(it)
    }


suspend fun importModrinth(path: String): Pair<MrModpackModel?, String> =
    if (path.pathInfo.extension == "mrpack")
    {
        importMrPackFile(path) to path
    }
    else importMrManifestFile(path) to path

suspend fun Pair<CfModpackModel?, String>.asSetOfCfProjects(
    onError: ErrorBlock,
    pakkuLock: PakkuLock,
    platforms: List<Platform>
): Set<Project>
{
    val (model, path) = this

    if (model == null)
    {
        onError.error(Error.CouldNotImport("Could not import from $path"))
        return setOf()
    }

    val projects = CurseForge.requestMultipleProjects(model.files.map { it.projectID })
    val projectFiles = CurseForge.requestMultipleProjectFiles(pakkuLock.getMcVersions(), pakkuLock.getLoaders(), model.files.map { it.fileID })

    projects.assignFiles(projectFiles, CurseForge)

    // Modrinth
    return if (Modrinth in platforms)
    {
        debug { println("Modrinth sub-import") }

        val slugs = projects.mapNotNull { project ->
            project.slug[CurseForge.serialName]
        }

        val mrProjects = Modrinth.requestMultipleProjectsWithFiles(
            pakkuLock.getMcVersions(), pakkuLock.getLoaders(), slugs, 1
        )

        projects.combineWith(mrProjects)
    }
    else projects
}


suspend fun Pair<MrModpackModel?, String>.asSetOfMrProjects(
    onError: ErrorBlock,
    pakkuLock: PakkuLock,
    platforms: List<Platform>
): Set<Project>
{
    val (model, path) = this

    if (model == null)
    {
        onError.error(Error.CouldNotImport("Could not import from $path"))
        return setOf()
    }

    val projects = Modrinth.requestMultipleProjectsWithFilesFromHashes(
        model.files.map { it.hashes.sha1 }, "sha1"
    )

    // CurseForge
    return if (CurseForge in platforms)
    {
        runBlocking {
            debug { println("CurseForge sub-import") }

            val slugs = projects.mapNotNull { project ->
                project.slug[Modrinth.serialName]
            }

            val cfProjects = slugs.map { slug ->
                async {
                    CurseForge.requestProjectFromSlug(slug)?.apply {
                        files += CurseForge.requestFilesForProject(
                            pakkuLock.getMcVersions(), pakkuLock.getLoaders(), this
                        )
                    }
                }
            }.awaitAll().filterNotNull()

            projects.combineWith(cfProjects).debug { println(it.map { p -> p.slug }) }
        }
    }
    else projects
}