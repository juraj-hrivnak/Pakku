package teksturepako.pakku.api.actions

sealed class Error(val message: String)
{
    // File
    class FileNotFound(file: String) : Error("File not found: $file")

    // Addition
    class ProjNotFound(message: String) : Error(message)
    class AlreadyAdded(message: String) : Error(message)
    class NotFoundOnPlatform(message: String) : Error(message)
    class NoFilesOnPlatform(message: String) : Error(message)
    class NoFiles(message: String) : Error(message)
    class FileNamesDoNotMatch(message: String) : Error(message)

    // Import
    class CouldNotImport(file: String) : Error("Could not import from: '$file'. It is not a proper modpack file.")
}
