package teksturepako.pakku.api.projects

import com.github.michaelbull.result.get
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.io.filterPath

enum class ProjectType(
    val serialName: String, val prettyName: String, private val defaultPath: String
)
{
    MOD("mods", "mod", "mods"),
    RESOURCE_PACK("resource_packs", "resource pack", "resourcepacks"),
    DATA_PACK("data_packs", "data pack", "datapacks"),
    WORLD("worlds", "world", "saves"),
    SHADER("shader_packs", "shader pack", "shaderpacks");

    fun getPathString(configFile: ConfigFile?): String
    {
        return if (configFile == null) return defaultPath else
        {
            filterPath(configFile.paths.getOrDefault(this.serialName, defaultPath)).get() ?: defaultPath
        }
    }
}