package teksturepako.pakku.api.actions

import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.cli.ui.dim
import teksturepako.pakku.cli.ui.getFlavoredSlug
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
open class ActionError(
    val rawMessage: String,
    val isWarning: Boolean = false,
)
{
    /** A message dedicated for the CLI. It should not be used outside of terminal. */
    open fun message(arg: String = ""): String = rawMessage

    // -- FILE --

    class FileNotFound(val file: String) : ActionError("File not found: '$file'")
    {
        override fun message(arg: String): String = "Project '$arg' not found."
    }

    class CouldNotRead(val file: String, val reason: String? = "") :
        ActionError("Could not read: '$file'. $reason")

    class ErrorWhileReading(val file: String, val reason: String? = "") :
        ActionError("Error occurred while reading: '$file'. $reason")

    class AlreadyExists(val file: String) :
        ActionError("File '$file' already exists.")

    // -- PROJECT FILE --

    class DownloadFailed(val path: Path?) :
        ActionError("Failed to download '$path'.")

    class NoHashes(val projectFile: ProjectFile) :
        ActionError("File '${projectFile.getPath()}' has no hashes.", isWarning = true)

    class HashFailed(val projectFile: ProjectFile, val originalHash: String, val newHash: String) :
        ActionError("""Failed to math hash for file '${projectFile.getPath()}'.
            | Original hash: $originalHash
            | New hash: $newHash
            """.trimMargin())

    class CouldNotSave(val path: Path?, val reason: String? = "") :
        ActionError(if (path != null) "Could not save: '$path'. $reason" else "Could not save file. $reason")

    // -- IMPORT --

    class CouldNotImport(val file: String) :
        ActionError("Could not import from: '$file'. It is not a proper modpack file.")

    // -- PROJECT --

    class ProjNotFound : ActionError("Project not found.")

    class ProjDiffTypes(val project: Project, val otherProject: Project) :
        ActionError("""Can not combine two projects of different types:
            | ${project.slug} ${project.type} + ${otherProject.slug} ${otherProject.type}
            """.trimMargin()
        )
        {
            override fun message(arg: String): String = """Can not combine two projects of different types:
            | ${project.getFlavoredSlug()} ${dim(project.type)} + ${otherProject.getFlavoredSlug()} ${dim(otherProject.type)}
            """.trimMargin()
        }

    class ProjDiffPLinks(val project: Project, val otherProject: Project) :
        ActionError("""Can not combine two projects with different pakku links:
            | ${project.slug} ${project.type} + ${otherProject.slug} ${otherProject.type}
            """.trimMargin()
        )
        {
            override fun message(arg: String): String = """Can not combine two projects with different pakku links:
                | ${project.getFlavoredSlug()} ${dim(project.type)} + ${otherProject.getFlavoredSlug()} ${dim(otherProject.type)}
                """.trimMargin()
        }

    // -- EXPORT --

    class NotRedistributable(val project: Project) :
        ActionError("${project.type} ${project.slug} can not be exported, because it is not redistributable.")
        {
            override fun message(arg: String): String =
                "${dim(project.type)} ${project.getFlavoredSlug()} can not be exported, because it is not redistributable."
        }

    // -- ADDITION --

    class AlreadyAdded(val project: Project) :
        ActionError("Can not add ${project.type} ${project.slug}. It is already added.")
        {
            override fun message(arg: String): String =
                "Could not add ${dim(project.type)} ${project.getFlavoredSlug()}. It is already added."
        }

    class NotFoundOn(val project: Project, val provider: IProjectProvider) :
        ActionError("${project.type} ${project.slug} was not found on ${provider.name}")
        {
            override fun message(arg: String): String =
                "${dim(project.type)} ${project.getFlavoredSlug()} was not found on ${provider.name}."
        }

    class NoFilesOn(val project: Project, val provider: IProjectProvider) :
        ActionError("No files for ${project.type} ${project.slug} found on ${provider.name}.")
        {
            override fun message(arg: String) =
                "No files for ${dim(project.type)} ${project.getFlavoredSlug()} found on ${provider.name}."
        }

    class NoFiles(val project: Project, val lockFile: LockFile) :
        ActionError(
            """No files found for ${project.type} ${project.slug}.
            | Your modpack requires Minecraft versions: ${lockFile.getMcVersions()} and loaders: ${lockFile.getLoaders()}.
            | Make sure the project complies these requirements.
            """.trimMargin()
        )
        {
            override fun message(arg: String) = """No files found for ${dim(project.type)} ${project.getFlavoredSlug()}.
            | Your modpack requires Minecraft versions: ${lockFile.getMcVersions()} and loaders: ${lockFile.getLoaders()}.
            | Make sure the project complies these requirements.
            """.trimMargin()
        }

    class FileNamesDoNotMatch(val project: Project) :
        ActionError(
            """${project.type} ${project.slug} versions do not match across platforms.
            | ${project.files.map { "${it.type}: ${it.fileName}" }}
            """.trimMargin()
        )
        {
            override fun message(arg: String) =
                """${dim(project.type)} ${project.getFlavoredSlug()} versions do not match across platforms.
                | ${project.files.map { "${it.type}: ${it.fileName}" }}
                """.trimMargin()
        }

    // -- REMOVAL --

    class ProjRequiredBy(val project: Project, val dependants: List<Project>) :
        ActionError("${project.type} ${project.slug} is required by ${dependants.map { it.slug }}", isWarning = true)
        {
            override fun message(arg: String) = "${dim(project.type)} ${project.getFlavoredSlug()} is required by " +
                    "${dependants.map { it.getFlavoredSlug() }}."
        }
}
