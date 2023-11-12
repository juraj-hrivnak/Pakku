package teksturepako.platforms.projects

enum class ProjectType
{
    MOD,
    RESOURCE_PACK,
    SHADER_PACK;

    fun getFolderName(): String
    {
        return when (this)
        {
            MOD -> "mods"
            RESOURCE_PACK -> "resourcepacks"
            SHADER_PACK -> "shaderpacks"
        }
    }

    fun getName(): String
    {
        return when (this)
        {
            MOD -> "mod"
            RESOURCE_PACK -> "resource pack"
            SHADER_PACK -> "shader pack"
        }
    }
}