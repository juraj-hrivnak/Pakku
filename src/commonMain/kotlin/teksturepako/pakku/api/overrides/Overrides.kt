package teksturepako.pakku.api.overrides

import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.io.filterPath
import kotlin.io.path.Path
import kotlin.io.path.pathString

fun readOverrides(overrides: List<String>): List<String> = overrides
    .mapNotNull { filterPath(it) }
    .map { Path(workingPath, it).pathString }

const val PAKKU_DIR = ".pakku"