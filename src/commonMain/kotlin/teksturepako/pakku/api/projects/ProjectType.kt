package teksturepako.pakku.api.projects

enum class ProjectType(name: String, val folderName: String)
{
    MOD("mod", "mods"),
    RESOURCE_PACK("resource pack", "resourcepacks"),
    WORLD("world", "saves"),
    SHADER("shader pack", "shaderpacks"),
}