package teksturepako.pakku.api.actions.export.rules

import com.github.michaelbull.result.Err
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.actions.ActionError.DownloadFailed
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.*

sealed class RuleContext
{
    /** Ignores this [rule context][RuleContext]. */
    fun ignore() =
        ruleResult("ignore", Packaging.Ignore)

    /** Creates a new JSON file at the specified [path] & returns a result. */
    inline fun <reified T> createJsonFile(
        value: T, path: String, vararg subpath: String, format: StringFormat = json
    ) = ruleResult("createJsonFile '$path'", Packaging.Action {
        val file = Path(workingPath, PAKKU_DIR, "temp", path, *subpath)

        file.tryToResult {
            it.createParentDirectories()
            file.writeText(format.encodeToString(value))
        }
    })

    /** Creates a new file at the specified [path] & returns a result. */
    fun createFile(bytes: ByteArray, path: String, vararg subpath: String): RuleResult
    {
        val file = Path(workingPath, PAKKU_DIR, "temp", path, *subpath)

        return ruleResult("createFile '$file'", Packaging.Action {
            file.tryToResult {
                it.createParentDirectories()
                file.writeBytes(bytes)
            }
        })
    }

    /**
     *  **Lazily** creates a new file at the specified [path]
     * if it does not already exist, and returns a result.
     */
    suspend fun createFile(
        bytesCallback: suspend () -> ByteArray?,
        path: String,
        vararg subpath: String
    ): RuleResult
    {
        val file = Path(workingPath, PAKKU_DIR, "temp", path, *subpath)

        return ruleResult("createFile '$file'", Packaging.Action {
            if (file.exists()) return@Action Err(ActionError.AlreadyExists(file.pathString))

            val bytes = bytesCallback.invoke()
                ?: return@Action Err(DownloadFailed(file))

            file.tryToResult {
                it.createParentDirectories()
                file.writeBytes(bytes)
            }
        })
    }

    /** Rule entry representing [project][Project]. */
    data class ExportingProject(
        val project: Project,
        val lockFile: LockFile,
        val configFile: ConfigFile,
    ) : RuleContext()
    {
        /** Sets the [project entry][RuleContext.ExportingProject] missing. */
        fun setMissing() =
            MissingProject(project, lockFile, configFile).ruleResult(
                "missing ${project.slug}", Packaging.EmptyAction
            )
    }

    data class ExportingOverride(
        val path: String,
        val type: OverrideType,
        val lockFile: LockFile,
        val configFile: ConfigFile,
    ) : RuleContext()
    {
        @OptIn(ExperimentalPathApi::class)
        fun export(): RuleResult
        {
            val inputPath = Path(path)
            val outputPath = Path(PAKKU_DIR, "temp", type.folderName, path)

            return ruleResult("export $type '$inputPath' to '$outputPath'", Packaging.Action {
                if (inputPath.isRegularFile())
                {
                    inputPath.tryToResult<Unit> {
                        outputPath.createParentDirectories()
                        it.copyTo(outputPath)
                    }
                }
                else if (inputPath.isDirectory())
                {
                    inputPath.tryToResult<Unit> {
                        outputPath.createParentDirectories()
                        it.copyToRecursively(outputPath, followLinks = true)
                    }
                }
                else Err(ActionError.CouldNotSave(inputPath))
            })
        }
    }

    data class MissingProject(
        val project: Project,
        val lockFile: LockFile,
        val configFile: ConfigFile,
    ) : RuleContext()
    {
        suspend fun exportAsOverrideFrom(
            platform: Platform,
            onExport: suspend (
                bytes: suspend () -> ByteArray?,
                fileName: String,
                overridesFolder: String
            ) -> RuleResult
        ): RuleResult
        {
            val projectFile = project.getFilesForPlatform(platform).firstOrNull()
                ?: return ignore()

            val result = onExport(
                { projectFile.url?.let { url -> Http().requestByteArray(url) } },
                projectFile.fileName,
                OverrideType.fromProject(project).folderName
            )

            return ruleResult("exportAsOverride ${result.message}", result.packaging)
        }
    }

    data object Finished : RuleContext()
}