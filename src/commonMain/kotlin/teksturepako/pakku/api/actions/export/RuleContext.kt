package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.getError
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.*

/**
 * Rule Context keeps track of currently exporting content.
 */
sealed class RuleContext(open val workingSubDir: String)
{
    fun getPath(path: String, vararg subpath: String) =
        Path(workingPath, PAKKU_DIR, "temp", workingSubDir, path, *subpath)

    /** Returns an [error][ActionError]. */
    fun error(error: ActionError) =
        ruleResult("error ${error.message}", Packaging.Error(error))

    /** Ignores this [rule context][RuleContext]. */
    fun ignore() =
        ruleResult("ignore", Packaging.Ignore)

    /** Creates a new JSON file at the specified [path] & returns a result. */
    inline fun <reified T> createJsonFile(
        value: T, path: String, vararg subpath: String, format: StringFormat = json
    ): RuleResult
    {
        val file = getPath(path, *subpath)

        return ruleResult("createJsonFile '$file'", Packaging.FileAction {
            file to file.tryToResult {
                it.createParentDirectories()
                it.writeText(format.encodeToString(value))
            }.getError()
        })
    }

    /** Creates a new file at the specified [path] & returns a result. */
    @Suppress("unused")
    fun createFile(bytes: ByteArray, path: String, vararg subpath: String): RuleResult
    {
        val file = getPath(path, *subpath)

        return ruleResult("createFile '$file'", Packaging.FileAction {
            file to file.tryToResult {
                it.createParentDirectories()
                it.writeBytes(bytes)
            }.getError()
        })
    }

    /**
     *  **Lazily** creates a new file at the specified [path]
     * if it does not already exist, and returns a result.
     */
    fun createFile(
        bytesCallback: suspend () -> ByteArray?,
        path: String,
        vararg subpath: String
    ): RuleResult
    {
        val file = getPath(path, *subpath)

        return ruleResult("createFile '$file'", Packaging.FileAction {
            if (file.exists()) return@FileAction file to AlreadyExists(file.pathString)

            val bytes = bytesCallback.invoke() ?: return@FileAction file to DownloadFailed(file)

            file to file.tryToResult {
                it.createParentDirectories()
                it.writeBytes(bytes)
            }.getError()
        })
    }

    /** Rule context representing a [project][Project]. */
    data class ExportingProject(
        val project: Project,
        val lockFile: LockFile,
        val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir)
    {
        /** Sets the [project entry][RuleContext.ExportingProject] missing. */
        fun setMissing(): RuleResult
        {
            if (!project.redistributable) return error(NotRedistributable(project))

            return MissingProject(project, lockFile, configFile, workingSubDir).ruleResult(
                "missing ${project.slug}", Packaging.EmptyAction
            )
        }


        suspend fun exportAsOverride(
            onExport: suspend (
                bytesCallback: suspend () -> ByteArray?,
                fileName: String,
                overridesFolder: String
            ) -> RuleResult
        ): RuleResult
        {
            val projectFile = Multiplatform.platforms.firstNotNullOfOrNull { platform ->
                project.getFilesForPlatform(platform).firstOrNull()
            } ?: return error(NoFiles(project, lockFile))

            val result = onExport(
                // Creates a callback to download the file lazily.
                { projectFile.url?.let { url -> Http().requestByteArray(url) } },
                projectFile.fileName,
                OverrideType.fromProject(project).folderName
            )

            return ruleResult("exportAsOverride ${result.message}", result.packaging)
        }
    }

    /** Rule context representing an 'override'. */
    data class ExportingOverride(
        val path: String,
        val type: OverrideType,
        val lockFile: LockFile,
        val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir)
    {
        @OptIn(ExperimentalPathApi::class)
        fun export(overridesDir: String? = type.folderName): RuleResult
        {
            val inputPath = Path(path)
            val outputPath = overridesDir?.let { getPath(it, path) } ?: getPath(path)

            val message = "export $type '$inputPath' to '$outputPath'"

            return ruleResult(message, Packaging.FileAction {
                when
                {
                    inputPath.isRegularFile() ->
                    {
                        outputPath to inputPath.tryToResult {
                            outputPath.createParentDirectories()
                            it.copyTo(outputPath)
                        }.getError()
                    }

                    inputPath.isDirectory()   ->
                    {
                        outputPath to inputPath.tryToResult {
                            outputPath.createParentDirectories()
                            it.copyToRecursively(outputPath, followLinks = true)
                        }.getError()
                    }

                    else                      ->
                    {
                        outputPath to CouldNotSave(inputPath)
                    }
                }
            })
        }
    }

    /** Rule context representing a [project override][ProjectOverride]. */
    data class ExportingProjectOverride(
        val projectOverride: ProjectOverride,
        val lockFile: LockFile,
        val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir)
    {
        fun export(overridesDir: String? = projectOverride.type.folderName): RuleResult
        {
            val file = overridesDir?.let {
                getPath(it, projectOverride.relativeOutputPath.pathString)
            } ?: getPath(projectOverride.relativeOutputPath.pathString)

            val message = "export ${projectOverride.type} '${projectOverride.path}' to '$file'"

            return ruleResult(message, Packaging.FileAction {
                if (file.exists())
                {
                    return@FileAction file to AlreadyExists(file.toString())
                }

                file to file.tryToResult {
                    it.createParentDirectories()
                    it.writeBytes(projectOverride.bytes)
                }.getError()
            })
        }
    }

    /** Rule context representing a [missing project][Project]. */
    data class MissingProject(
        val project: Project,
        val lockFile: LockFile,
        val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir)
    {
        suspend fun exportAsOverrideFrom(
            platform: Platform,
            onExport: suspend (
                bytesCallback: suspend () -> ByteArray?,
                fileName: String,
                overridesDir: String
            ) -> RuleResult
        ): RuleResult
        {
            val projectFile = project.getFilesForPlatform(platform).firstOrNull()
                ?: return ignore()

            val result = onExport(
                // Creates a callback to download the file lazily.
                { projectFile.url?.let { url -> Http().requestByteArray(url) } },
                projectFile.fileName,
                OverrideType.fromProject(project).folderName
            )

            return ruleResult(
                "exportAsOverrideFrom ${platform.name} ${result.message}",
                result.packaging
            )
        }
    }

    /** Rule context indicating that all other actions has been finished. */
    data class Finished(override val workingSubDir: String) : RuleContext(workingSubDir)
}