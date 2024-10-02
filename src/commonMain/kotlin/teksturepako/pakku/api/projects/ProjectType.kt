package teksturepako.pakku.api.projects

import kotlinx.serialization.json.JsonNames
import teksturepako.pakku.api.data.ConfigFile
import kotlin.reflect.KProperty1

enum class ProjectType(
    val prettyName: String, private val defaultPath: String
)
{
    MOD("mod", "mods"),
    RESOURCE_PACK("resource pack", "resourcepacks"),
    DATA_PACK("data pack", "datapacks"),
    WORLD("world", "saves"),
    SHADER("shader pack", "shaderpacks");

    fun getPathString(configFile: ConfigFile?): String
    {
        return if (configFile == null) return defaultPath else
        {
            configFile.paths.getOrDefault(this, defaultPath)
        }
    }
}