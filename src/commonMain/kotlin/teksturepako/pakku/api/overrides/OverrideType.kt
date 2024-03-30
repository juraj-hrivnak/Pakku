package teksturepako.pakku.api.overrides

enum class OverrideType(name: String, val folderName: String)
{
    OVERRIDE("override", "overrides"),
    SERVER_OVERRIDE("server override", "server-overrides"),
    CLIENT_OVERRIDE("client override", "client-overrides"),
}