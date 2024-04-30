package teksturepako.pakku.api.actions

sealed class PError(val message: String)
{
    // File
    class FileNotFound(file: String) : PError("File not found: '$file'")
    class CouldNotRead(file: String, reason: String? = "") : PError("Could not read: '$file'. $reason")

    // Addition
    class ProjNotFound(message: String) : PError(message)
    class AlreadyAdded(message: String) : PError(message)
    class NotFoundOnPlatform(message: String) : PError(message)
    class NoFilesOnPlatform(message: String) : PError(message)
    class NoFiles(message: String) : PError(message)
    class FileNamesDoNotMatch(message: String) : PError(message)

    // Import
    class CouldNotImport(file: String) : PError("Could not import from: '$file'. It is not a proper modpack file.")
}
