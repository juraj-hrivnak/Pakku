package teksturepako.pakku.api.overrides

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.debugIfNotEmpty
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.toPrettyString
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

suspend fun readManualOverrides(
    configFile: ConfigFile?,
    allowedTypes: Set<OverrideType>? = null,
): Set<ManualOverride> = readManualOverridesFrom(Path(workingPath), configFile, allowedTypes)

suspend fun readManualOverridesFrom(
    path: Path,
    configFile: ConfigFile?,
    allowedTypes: Set<OverrideType>? = null,
): Set<ManualOverride> = (allowedTypes ?: OverrideType.entries)
    .map { ovType ->
        Path(path.pathString, PAKKU_DIR, ovType.folderName)
    }
    .mapNotNull { dir ->
        dir.tryOrNull {
            toFile().walkTopDown().map { file: File ->
                file.toPath()
            }
        }
    }
    .flatMap { pathSequence ->
        coroutineScope {
            pathSequence.toSet().map { path ->
                async {
                    ManualOverride.fromPath(path, configFile)
                }
            }
        }
    }
    .awaitAll()
    .filterNotNull()
    .debugIfNotEmpty {
        println("readManualOverrides = ${it.toPrettyString()}")
    }
    .toSet()

@Suppress("unused")
suspend fun copyManualOverrideDirectories(inputPath: Path, outputPath: Path) = OverrideType.entries
    .map { ovType ->
        Path(inputPath.pathString, PAKKU_DIR, ovType.folderName) to Path(outputPath.pathString, PAKKU_DIR, ovType.folderName)
    }
    .forEach { (input, output) ->
        input.tryOrNull { toFile().copyRecursively(output.toFile(), overwrite = true) }
    }