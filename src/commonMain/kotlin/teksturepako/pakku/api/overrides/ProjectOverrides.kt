package teksturepako.pakku.api.overrides

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.debug
import teksturepako.pakku.debugIf
import teksturepako.pakku.io.tryOrNull
import java.io.File
import kotlin.io.path.Path

suspend fun readProjectOverrides(configFile: ConfigFile?): Set<ProjectOverride> = OverrideType.entries
    .flatMap { ovType ->
        ProjectType.entries.map { projType ->
            Path(workingPath, PAKKU_DIR, ovType.folderName, projType.getPathString(configFile))
        }
    }
    .mapNotNull { path ->
        path.tryOrNull {
            it.toFile().walkTopDown().map { file: File ->
                file.toPath()
            }
        }
    }
    .flatMap { pathSequence ->
        coroutineScope {
            pathSequence.toSet().map { path ->
                path.debug(::println)
                async {
                    ProjectOverride.fromPath(path, configFile)
                }
            }
        }
    }
    .awaitAll()
    .filterNotNull()
    .toSet()
    .debugIf({ it.isNotEmpty() }) {
        println("readProjectOverrides = ${it.map { projectOverride -> projectOverride.path }}")
    }
