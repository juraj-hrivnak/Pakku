package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

@Suppress("MemberVisibilityCanBePrivate")
sealed class ActionError(val message: String)
{
    // -- FILE --

    class FileNotFound(val file: String) : ActionError("File not found: '$file'")

    class CouldNotRead(val file: String, val reason: String? = "") : ActionError("Could not read: '$file'. $reason")

    // -- ADDITION --

    class ProjNotFound : ActionError("Project not found")

    class AlreadyAdded(val project: Project) : ActionError("Could not add ${project.slug}. It is already added.")

    class NotFoundOnPlatform(val project: Project, val platform: Platform) :
        ActionError("${project.slug} was not found on ${platform.name}")

    class NoFilesOnPlatform(val project: Project, val platform: Platform) :
        ActionError("No files for ${project.slug} found on ${platform.name}")

    class NoFiles(val project: Project, val lockFile: LockFile) :
        ActionError("""No files found for ${project.slug}.
            | Your modpack requires Minecraft versions: ${lockFile.getMcVersions()};
            | Project has ${project.files.map { it.mcVersions }}.
            | Your modpack requires loaders: ${lockFile.getLoaders()};
            | Project has ${project.files.map { it.loaders }}.
            """.trimMargin())

    class FileNamesDoNotMatch(val project: Project) :
        ActionError("""${project.slug} versions do not match across platforms.
            | ${project.files.map { "${it.type}: ${it.fileName}" }}
            """.trimMargin())

    // -- IMPORT --

    class CouldNotImport(val file: String) :
        ActionError("Could not import from: '$file'. It is not a proper modpack file.")
}
