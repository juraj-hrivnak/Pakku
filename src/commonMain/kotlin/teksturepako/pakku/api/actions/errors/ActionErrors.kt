@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package teksturepako.pakku.api.actions.errors

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.cli.ui.dim
import teksturepako.pakku.cli.ui.getFlavoredSlug
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.toMsg
import java.nio.file.Path

data class DirectoryNotEmpty(val file: String) : ActionError()
{
    override val rawMessage = "Directory '$file' is not empty."
}

data class FileNotFound(val file: String) : ActionError()
{
    override val rawMessage = "File '$file' not found."
}

data class CouldNotRead(val file: String, val reason: String? = "") : ActionError()
{
    override val rawMessage = "Could not read: '$file'. $reason"
}

data class ErrorWhileReading(val file: String, val reason: String? = "") : ActionError()
{
    override val rawMessage = "Error occurred while reading: '$file'. $reason"
}

data class AlreadyExists(val file: String) : ActionError()
{
    override val rawMessage = "File '$file' already exists."
}

// -- PROJECT FILE --

data class NoUrl(val projectFile: ProjectFile) : ActionError()
{
    override val rawMessage = "${projectFile.fileName} has no URL."
}

data class DownloadFailed(val path: Path?, val retryNumber: Int = 0) : ActionError()
{
    override val rawMessage = "Failed to download '$path'. ${if (retryNumber > 0) "Retry number $retryNumber." else ""}"
}

data class NoHashes(val path: Path?) : ActionError()
{
    override val rawMessage = "File '$path' has no hashes."
    override val severity = ErrorSeverity.WARNING
}

data class HashMismatch(val path: Path?, val originalHash: String, val newHash: String) : ActionError()
{
    override val rawMessage = message(
        "Failed to math hash for file '$path'.",
        "Original hash: $originalHash",
        "New hash: $newHash",
        newlines = true,
    )
}

data class CouldNotSave(val path: Path?, val reason: String? = "") : ActionError()
{
    override val rawMessage = if (path != null) "Could not save: '$path'. $reason" else "Could not save file. $reason"
}

// -- IMPORT --

data class CouldNotImport(val file: String) : ActionError()
{
    override val rawMessage = "Could not import from: '$file'. It is not a proper modpack file."
}

// -- PROJECT --

data class ProjNotFound(val projectInput: String? = null, val project: Project? = null) : ActionError()
{
    override val rawMessage = if (project == null)
    {
        if (projectInput.isNullOrEmpty()) "Project not found." else "Project '$projectInput' not found."
    }
    else "Project ${project.type} ${project.slug} not found."

    override fun message(arg: String) = if (project == null)
    {
        if (projectInput.isNullOrEmpty()) "Project not found." else "Project '$projectInput' not found."
    }
    else "Project ${project.getFullMsg()} not found."
}

data class ProjDiffTypes(val project: Project, val otherProject: Project) : ActionError()
{
    override val rawMessage = message(
        "Can not combine two projects of different types:",
        "${project.slug} ${project.type} + ${otherProject.slug} ${otherProject.type}",
        newlines = true
    )

    override fun message(arg: String): String = message(
        "Can not combine two projects of different types:",
        "${project.getFlavoredSlug()} ${dim(project.type)} + ${otherProject.getFlavoredSlug()} ${dim(otherProject.type)}",
        newlines = true
    )
}

data class ProjDiffPLinks(val project: Project, val otherProject: Project) : ActionError()
{
    override val rawMessage = message(
        "Can not combine two projects with different pakku links:",
        "${project.slug} ${project.type} + ${otherProject.slug} ${otherProject.type}",
        newlines = true
    )

    override fun message(arg: String): String = message(
        "Can not combine two projects with different pakku links:",
        "${project.getFlavoredSlug()} ${dim(project.type)} + ${otherProject.getFlavoredSlug()} ${dim(otherProject.type)}",
        newlines = true
    )
}

// -- EXPORT --

data class NotRedistributable(val project: Project) : ActionError()
{
    override val rawMessage = "${project.type} ${project.slug} can not be exported, because it is not redistributable."

    override fun message(arg: String): String =
        "${dim(project.type)} ${project.getFlavoredSlug()} can not be exported, because it is not redistributable."
}

data class CouldNotExport(val profile: ExportProfile, val modpackFileName: String, val reason: String?) : ActionError()
{
    override val rawMessage = "Profile ${profile.name} ('$modpackFileName') could not be exported. $reason"
    override fun message(arg: String): String = "(${dim(modpackFileName)}) could not be exported. $reason"
    override val severity = ErrorSeverity.FATAL
}

data class IOExportingError(val underlyingError: ActionError) : ActionError()
{
    override val rawMessage = message(
        "There was an file IO error while exporting. ",
        underlyingError.rawMessage
    )

    override fun message(arg: String): String = message(
        "There was an file IO error while exporting. ",
        underlyingError.message()
    )

    override val severity = underlyingError.severity
}

data class ExportingError(val underlyingError: ActionError) : ActionError()
{
    override val rawMessage = message(
        "There was an error while exporting. ",
        underlyingError.rawMessage
    )

    override fun message(arg: String): String = message(
        "There was an error while exporting. ",
        underlyingError.message()
    )

    override val severity = underlyingError.severity
}

// -- ADDITION --

data class AlreadyAdded(val project: Project) : ActionError()
{
    override val rawMessage = "${project.type} ${project.slug} is already added."

    override val severity = ErrorSeverity.NOTICE

    override fun message(arg: String): String = "${dim(project.type)} ${project.getFlavoredSlug()} is already added."
}

data class NotFoundOn(val project: Project, val provider: Provider) : ActionError()
{
    override val rawMessage = "${project.type} ${project.slug} was not found on ${provider.name}"

    override fun message(arg: String): String =
        "${dim(project.type)} ${project.getFlavoredSlug()} was not found on ${provider.name}."
}

data class NoFilesOn(val project: Project, val provider: Provider) : ActionError()
{
    override val rawMessage = "No files for ${project.type} ${project.slug} found on ${provider.name}."

    override fun message(arg: String) =
        "No files for ${dim(project.type)} ${project.getFlavoredSlug()} found on ${provider.name}."
}

data class NoFiles(val project: Project, val lockFile: LockFile) : ActionError()
{
    override val rawMessage = message(
        "No files found for ${project.type} ${project.slug}.",
        "Your modpack requires Minecraft versions: ${lockFile.getMcVersions()} and loaders: ${lockFile.getLoaders()}.",
        "Make sure the project complies these requirements.",
        newlines = true
    )

    override fun message(arg: String) = message(
        "No files found for ${dim(project.type)} ${project.getFlavoredSlug()}.",
        "Your modpack requires Minecraft versions: ${lockFile.getMcVersions()} and loaders: ${lockFile.getLoaders()}.",
        "Make sure the project complies these requirements.",
        newlines = true
    )
}

data class VersionsDoNotMatch(val project: Project) : ActionError()
{
    override val rawMessage = message(
        project.type, project.slug, "versions do not match across platforms.",
        project.files.map { "${it.type}: ${it.fileName}" }.toString(),
        newlines = true
    )

    override fun message(arg: String) = message(
        "${dim(project.type)} ${project.getFlavoredSlug()} versions do not match across platforms.",
        project.files.map { "${it.type}: ${it.fileName}" }.toString(),
        newlines = true
    )

    override fun shortMessage(arg: String) = "Versions do not match across platforms."

    override val severity = ErrorSeverity.WARNING
}

// -- REMOVAL --

data class ProjRequiredBy(val project: Project, val dependants: List<Project>) : ActionError()
{
    override val rawMessage = message(project.type, project.slug, "is required by ${dependants.map { it.slug }}")

    override fun message(arg: String) = message(
        "${dim(project.type)} ${project.getFlavoredSlug()} is required by ",
        dependants.map { it.getFlavoredSlug() }.toMsg(), "."
    )

    override val severity = ErrorSeverity.WARNING
}
