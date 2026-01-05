package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.onFailure
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.errors.*
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs.cacheDir
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.requestByteArray
import teksturepako.pakku.api.overrides.ManualOverride
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.io.copyFileTo
import teksturepako.pakku.io.copyRecursivelyTo
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.*

/**
 * Rule Context keeps track of currently exporting content.
 */
sealed class RuleContext(
    open val workingSubDir: String,
    open val lockFile: LockFile,
    open val configFile: ConfigFile,
    open val clientOnly: Boolean = false
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
        val outputPath = getPath(path, *subpath)

        return ruleResult("createJsonFile '$outputPath'", Packaging.FileAction {
            outputPath.tryToResult { createParentDirectories() }
                .onFailure { error ->
                    if (error !is AlreadyExists) return@FileAction outputPath to error
                }

            outputPath to outputPath.tryToResult { writeText(format.encodeToString(value)) }
                .getError()
        })
    }

    /** Creates a new file at the specified [path] & returns a result. */
    @Suppress("unused")
    fun createFile(bytes: ByteArray, path: String, vararg subpath: String): RuleResult
    {
        val outputPath = getPath(path, *subpath)

        return ruleResult("createFile '$outputPath'", Packaging.FileAction {
            outputPath.tryToResult { createParentDirectories() }
                .onFailure { error ->
                    if (error !is AlreadyExists) return@FileAction outputPath to error
                }

            outputPath to outputPath.tryToResult { writeBytes(bytes) }
                .getError()
        })
    }

    /**
     *  **Lazily** creates a new file at the specified [path]
     * if it does not already exist, and returns a result.
     */
    fun createFile(
        bytesCallback: suspend () -> Result<ByteArray, ActionError>?,
        path: String,
        vararg subpath: String
    ): RuleResult
    {
        val outputPath = getPath(path, *subpath)

        return ruleResult("createFile '$outputPath'", Packaging.FileAction {
            if (outputPath.exists()) return@FileAction outputPath to AlreadyExists(outputPath.pathString)

            val bytes = bytesCallback.invoke()?.get() ?: return@FileAction outputPath to DownloadFailed(outputPath)

            outputPath.tryToResult { createParentDirectories() }
                .onFailure { error ->
                    if (error !is AlreadyExists) return@FileAction outputPath to error
                }

            outputPath to outputPath.tryToResult { writeBytes(bytes) }
                .getError()
        })
    }

    /** Rule context representing a [project][Project]. */
    data class ExportingProject(
        val project: Project,
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String,
        override val clientOnly: Boolean = false
    ) : RuleContext(workingSubDir, lockFile, configFile, clientOnly)
    {
        /** Sets the [project entry][RuleContext.ExportingProject] missing. */
        fun setMissing(): RuleResult = MissingProject(project, lockFile, configFile, workingSubDir, clientOnly)
            .ruleResult("missing ${project.slug}", Packaging.EmptyAction)
        
        suspend fun exportAsOverride(
            force: Boolean = false,
            onExport: suspend (
                bytesCallback: suspend () -> Result<ByteArray, ActionError>?,
                fileName: String,
                overridesFolder: String
            ) -> RuleResult
        ): RuleResult
        {
            if (!project.redistributable && !force) return error(NotRedistributable(project))

            val projectFile = project.getLatestFile(Provider.providers) ?: return error(NoFiles(project, lockFile))

            val result = onExport(
                // Creates a callback to download the file lazily.
                { projectFile.url?.let { url -> requestByteArray(url) } },
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
        override val workingSubDir: String,
        override val clientOnly: Boolean = false
    ) : RuleContext(workingSubDir, lockFile, configFile, clientOnly)
    {
        fun export(
            overridesDir: String? = type.folderName,
            allowedTypes: Set<OverrideType>? = null
        ): RuleResult
        {
            if (allowedTypes != null && type !in allowedTypes) return ignore()

            val inputPath = Path(workingPath, path)
            val outputPath = overridesDir?.let { getPath(it, path) } ?: getPath(path)

            val message = "export $type '$inputPath' to '$outputPath'"

            return ruleResult(message, Packaging.FileAction {
                outputPath to inputPath.copyRecursivelyTo(outputPath, cleanUp = false)
            })
        }
    }

    /** Rule context representing a [manual override][ManualOverride]. */
    data class ExportingManualOverride(
        val manualOverride: ManualOverride,
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String,
        override val clientOnly: Boolean = false
    ) : RuleContext(workingSubDir, lockFile, configFile, clientOnly)
    {
        fun export(
            overridesDir: String? = manualOverride.type.folderName,
            allowedTypes: Set<OverrideType>? = null
        ): RuleResult
        {
            if (allowedTypes != null && manualOverride.type !in allowedTypes) return ignore()

            val outputPath = overridesDir
                ?.let { getPath(it, manualOverride.relativeOutputPath.pathString) }
                ?: getPath(manualOverride.relativeOutputPath.pathString)

            val message = "export ${manualOverride.type} '${manualOverride.path}' to '$outputPath'"

            return ruleResult(message, Packaging.FileAction {
                outputPath.tryToResult { createParentDirectories() }
                    .onFailure { error ->
                        if (error !is AlreadyExists) return@FileAction outputPath to error
                    }

                outputPath to manualOverride.path.copyFileTo(outputPath) { }
            })
        }
    }

    /** Rule context representing a [missing project][Project]. */
    data class MissingProject(
        val project: Project,
        override val lockFile: LockFile,
        override val configFile: ConfigFile,
        override val workingSubDir: String,
        override val clientOnly: Boolean = false
    ) : RuleContext(workingSubDir, lockFile, configFile, clientOnly)
    {
        suspend fun exportAsOverrideFrom(
            provider: Provider,
            onExport: suspend (
                bytesCallback: suspend () -> Result<ByteArray, ActionError>?,
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
                { projectFile.url?.let { url -> requestByteArray(url) } },
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
                bytesCallback: suspend () -> Result<ByteArray, ActionError>?,
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
                {
                    projectFile.url?.let { url ->
                        requestByteArray(url)
                    }
                },
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
        override val workingSubDir: String,
        override val clientOnly: Boolean = false
    ) : RuleContext(workingSubDir, lockFile, configFile, clientOnly)
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
