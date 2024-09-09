@file:Suppress("MemberVisibilityCanBePrivate")

package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.michaelbull.result.Result
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.Dirs.PAKKU_DIR
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.io.decodeToResult
import teksturepako.pakku.io.writeToFile
import kotlin.io.path.Path

@Serializable
data class CliConfig(
    val theme: String? = "default",
    @SerialName("ansi_level") val ansiLevel: String? = null
)
{
    fun toTerminal() = Terminal(
        theme = when (theme?.lowercase())
        {
            "default" -> CliThemes.Default
            "ascii"   -> CliThemes.Ascii

            else      -> CliThemes.Ascii
        },
        ansiLevel = this.ansiLevel?.let { AnsiLevel.valueOf(it.uppercase()) }
    )

    companion object
    {
        const val FILE_NAME = "cli-config.json"
        val filePath = Path(workingPath, PAKKU_DIR, FILE_NAME)

        suspend fun readToResult(): Result<CliConfig, ActionError> = decodeToResult<CliConfig>(filePath)
    }

    suspend fun write() = writeToFile(this, filePath.toString(), overrideText = true, format = jsonEncodeDefaults)
}
