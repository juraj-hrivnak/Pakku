package teksturepako.pakku.api.overrides

import com.github.michaelbull.result.get
import teksturepako.pakku.io.filterPath

fun filterOverrides(overrides: List<String>): List<String> = overrides.mapNotNull { filterPath(it).get() }
