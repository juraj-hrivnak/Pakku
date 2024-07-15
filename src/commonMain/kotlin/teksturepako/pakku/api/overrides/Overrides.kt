package teksturepako.pakku.api.overrides

import teksturepako.pakku.io.filterPath

fun filterOverrides(overrides: List<String>): List<String> = overrides.mapNotNull { filterPath(it) }
