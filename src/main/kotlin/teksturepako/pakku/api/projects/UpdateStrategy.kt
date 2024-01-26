package teksturepako.pakku.api.projects

enum class UpdateStrategy(name: String, val short: String)
{
    LATEST("latest", "^"),
    SAME_LATEST("same_latest", "★^"), // TODO: To be implemented
    NONE("none", "✖^")
}