package teksturepako.pakku.api.projects

enum class UpdateStrategy(serialName: String)
{
    LATEST("latest"),
    FLEXVER("flexver"),
    NONE("none")
}