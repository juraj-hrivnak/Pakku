package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.getError
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.cacheDir
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.ProjectOverride
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.*

/**
 * Rule Context keeps track of currently exporting content.
 */
sealed class RuleContext(
    open val workingSubDir: String,
    open val lockFile: LockFile,
    open val configFile: ConfigFile
)
{
    fun getPath(path: String, vararg subpath: String) =
        Path(cacheDir.pathString, workingSubDir, path, *subpath)

    fun getPath() = Path(cacheDir.pathString, workingSubDir)

    /** Returns an [error][ActionError]. */
    fun error(error: ActionError) = ruleResult("error ${error.rawMessage}", Packaging.Error(error))

    /** Ignores this [rule context][RuleContext]. */
    fun ignore() = ruleResult("ignore", Packaging.Ignore)

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
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir, lockFile, configFile)
    {
        /** Sets the [project entry][RuleContext.ExportingProject] missing. */
        fun setMissing(): RuleResult = MissingProject(
            project, lockFile, configFile, workingSubDir
        ).ruleResult("missing ${project.slug}", Packaging.EmptyAction)
        
        suspend fun exportAsOverride(
            force: Boolean = false,
            onExport: suspend (
                bytesCallback: suspend () -> ByteArray?,
                fileName: String,
                overridesFolder: String
            ) -> RuleResult
        ): RuleResult
        {
            if (!project.redistributable && !force) return error(NotRedistributable(project))

            val projectFile =
                project.getLatestFile(Provider.providers) ?: return error(NoFiles(project, lockFile))

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
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir, lockFile, configFile)
    {
        fun export(overridesDir: String? = type.folderName): RuleResult
        {
            val inputPath = Path(workingPath, path)
            val outputPath = overridesDir?.let { getPath(it, path) } ?: getPath(path)

            val message = "export $type '$inputPath' to '$outputPath'"

            return ruleResult(message, Packaging.FileAction {
                when
                {
                    inputPath.isRegularFile() ->
                    {
                        outputPath to inputPath.tryToResult {
                            outputPath.createParentDirectories()
                            it.copyTo(outputPath, overwrite = true)
                        }.getError()
                    }

                    inputPath.isDirectory()   ->
                    {
                        outputPath to inputPath.tryToResult {
                            outputPath.createParentDirectories()
                            it.toFile().copyRecursively(outputPath.toFile(), overwrite = true)
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
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir, lockFile, configFile)
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
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir, lockFile, configFile)
    {
        suspend fun exportAsOverrideFrom(
            provider: Provider,
            onExport: suspend (
                bytesCallback: suspend () -> ByteArray?,
                fileName: String,
                overridesDir: String
            ) -> RuleResult
        ): RuleResult
        {
            if (!project.redistributable) return error(NotRedistributable(project))

            val projectFile = project.getFilesForProvider(provider).firstOrNull()
                ?: return error(NoFilesOn(project, provider))

            val result = onExport(
                // Creates a callback to download the file lazily.
                { projectFile.url?.let { url -> Http().requestByteArray(url) } },
                projectFile.fileName,
                OverrideType.fromProject(project).folderName
            )

            return ruleResult(
                "exportAsOverrideFrom ${provider.name} ${result.message}",
                result.packaging
            )
        }

        suspend fun exportAsOverride(
            force: Boolean = false,
            excludedProviders: Set<Provider> = setOf(),
            onExport: suspend (
                bytesCallback: suspend () -> ByteArray?,
                fileName: String,
                overridesFolder: String
            ) -> RuleResult
        ): RuleResult
        {
            if (!project.redistributable && !force) return error(NotRedistributable(project))

            val projectFile = project.getLatestFile(Provider.providers - excludedProviders)
                ?: return error(NoFiles(project, lockFile))

            val result = onExport(
                // Creates a callback to download the file lazily.
                { projectFile.url?.let { url -> Http().requestByteArray(url) } },
                projectFile.fileName,
                OverrideType.fromProject(project).folderName
            )

            return ruleResult("exportAsOverride ${result.message}", result.packaging)
        }
    }

    /** Rule context indicating that all other actions have been finished. */
    data class Finished(
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String
    ) : RuleContext(workingSubDir, lockFile, configFile)
    {
        fun replaceText(vararg pairs: Pair<String, String>): RuleResult
        {
            val message = "replaceText ${pairs.joinToString(", ", "[", "]") { "'${it.first}' -> '${it.second}'" }}"
            return ruleResult(message, Packaging.Action {
                getPath().tryToResult { path ->
                    for (file in path.toFile().walkTopDown())
                    {
                        if (!file.isFile || file.extension in listOf("jar", "zip")) continue

                        val text = file.readText()

                        if (pairs.map { it.first }.none { it in text }) continue

                        val replacedText = pairs.fold(text) { acc, (variable, content) ->
                            acc.replace(variable, content)
                        }

                        file.writeText(replacedText)
                    }
                }.getError()
            })
        }
    }
}
