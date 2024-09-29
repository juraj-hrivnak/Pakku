package teksturepako.pakku.api.projects

import teksturepako.pakku.api.data.ConfigFile
import kotlin.reflect.KProperty1

enum class ProjectType(
    val prettyName: String, private val defaultPath: String, private val pathConfig: KProperty1<ConfigFile, String?>
)
{
    MOD("mod", "mods", ConfigFile::modsPath),
    RESOURCE_PACK("resource pack", "resourcepacks", ConfigFile::resourcePacksPath),
    DATA_PACK("data pack", "datapacks", ConfigFile::dataPacksPath),
    WORLD("world", "saves", ConfigFile::worldsPath),
    SHADER("shader pack", "shaderpacks", ConfigFile::shadersPath);

    fun getPathString(configFile: ConfigFile?): String
    {
        return if (configFile == null) return defaultPath else
        {
            pathConfig.get(configFile) ?: defaultPath
        }
    }
}