package teksturepako.pakku.io

import korlibs.io.async.launch
import korlibs.io.file.VfsFile
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.file.std.createZipFromTree
import korlibs.io.file.std.localCurrentDirVfs
import korlibs.io.file.std.openAsZip
import korlibs.io.stream.openAsync
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import teksturepako.pakku.api.data.PakkuException
import teksturepako.pakku.api.overrides.Overrides.PROJECT_OVERRIDES_FOLDER
import teksturepako.pakku.api.overrides.Overrides.ProjectOverride

suspend fun unzip(path: String): VfsFile
{
    val file = localCurrentDirVfs[path]
    return file.openAsZip()
}

suspend fun zip(
    outputFileName: String,
    extension: String = "zip",
    overrides: List<String>,
    projectOverrides: Array<ProjectOverride> = arrayOf(),
    vararg create: Pair<String, Any>
): Result<String> = coroutineScope {
    val outputFile = kotlin.runCatching { localCurrentDirVfs["$outputFileName.$extension"] }.getOrElse {
        return@coroutineScope Result.failure(PakkuException("Error: Could not create output zip file named: " +
                "'$outputFileName'"))
    }
    val archive = MemoryVfsMix(*create)

    overrides.map { path ->
        launch {
            val vfs = localCurrentDirVfs[path]
            if (vfs.exists()) vfs.copyToRecursively(archive["overrides"][path])
        }
    }.joinAll()

//    projectOverrides.map { projectOverride ->
//        launch {
//            val vfs = localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${projectOverride.type.folderName}/${projectOverride.fileName}"]
//            if (vfs.exists())
//            {
//                vfs.parent.mkdirs()
//                vfs.copyToRecursively(archive["overrides"][projectOverride.type.folderName][projectOverride.fileName])
//            }
//        }
//    }.joinAll()

    val zipBytes = kotlin.runCatching { archive.createZipFromTree().openAsync() }.getOrElse {
        return@coroutineScope Result.failure(PakkuException("Error: Could not create zip: ${it.message}"))
    }

    try
    {
        outputFile.delete()
        outputFile.writeStream(zipBytes)

        zipBytes.close()
    }
    catch (e: Exception)
    {
        return@coroutineScope Result.failure(PakkuException("Error: Could not write zip: ${e.message}"))
    }

    return@coroutineScope Result.success(outputFile.relativePathTo(localCurrentDirVfs["."])!!)
}