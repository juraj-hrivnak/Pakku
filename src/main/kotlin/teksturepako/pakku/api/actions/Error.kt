package teksturepako.pakku.api.actions

sealed class Error(val message: String)
{
    // Addition
    class ProjNotFound(message: String) : Error(message)
    class AlreadyAdded(message: String) : Error(message)
    class NotFoundOnPlatform(message: String) : Error(message)
    class NoFilesOnPlatform(message: String) : Error(message)
    class NoFiles(message: String) : Error(message)
    class FileNamesDoNotMatch(message: String) : Error(message)

    // Import
    class CouldNotImport(message: String) : Error(message)
}