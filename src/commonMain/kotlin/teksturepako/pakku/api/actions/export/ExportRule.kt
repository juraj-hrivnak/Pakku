package teksturepako.pakku.api.actions.export

import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.overrides.PAKKU_DIR
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.io.tryToResult
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

fun interface ExportRule
{
    suspend fun onExport(ruleContext: Rule): RuleResult
}

sealed class Rule
{
    data class Entry(
        val project: Project,
        val lockFile: LockFile,
        val configFile: ConfigFile,
        val platform: Platform,
    ) : Rule()

    data object Finished : Rule()
}

data class RuleResult(
    val name: String,
    val ruleContext: Rule,
    val packagingAction: PackagingAction,
    val project: Project? = null,
)
{
    override fun toString() = buildString {
        append(packagingAction::class.simpleName)
        append(" ")
        append(name)
        if (project != null)
        {
            append(" ")
            append(project.name.values.firstOrNull())
        }
    }
}

sealed class PackagingAction
{
    data object Ignore : PackagingAction()
    data class Action(val action: suspend () -> Unit) : PackagingAction()
}

fun Rule.Entry.export(action: suspend () -> Unit) =
    RuleResult("export", this, PackagingAction.Action(action), this.project)

fun Rule.ignore() = RuleResult("ignore", this, PackagingAction.Ignore)

inline fun <reified T> Rule.createJsonFile(value: T, path: String, format: StringFormat = json) =
    RuleResult("createJsonFile", this, PackagingAction.Action {
        val file = Path(workingPath, PAKKU_DIR, "temp", path)

        file.tryToResult {
            it.createParentDirectories()
            file.writeText(format.encodeToString(value))
        }
    })

fun Rule.createFile(bytes: ByteArray, path: String) =
    RuleResult("createJsonFile", this, PackagingAction.Action {
        val file = Path(workingPath, PAKKU_DIR, "temp", path)

        file.tryToResult {
            it.createParentDirectories()
            file.writeBytes(bytes)
        }
    })
