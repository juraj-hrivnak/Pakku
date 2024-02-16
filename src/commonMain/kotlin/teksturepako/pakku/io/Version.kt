package teksturepako.pakku.io

import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.readProperties

suspend fun getPakkuVersion(): String?
{
    return runCatching { resourcesVfs["version.properties"].readProperties()["version"] }.getOrNull()
}