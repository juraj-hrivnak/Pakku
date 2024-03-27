package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

suspend fun promptForProject(platform: Platform, terminal: Terminal, lockFile: LockFile, fileId: String? = null): Project?
{
    val prompt = StringPrompt("Specify ${platform.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return null

    val splitArg = prompt.split(":")
    val input: String = splitArg[0]
    val fileIdArg: String? = splitArg.getOrNull(1) ?: fileId

    return Multiplatform.requestProjectWithFiles(
        lockFile.getMcVersions(), lockFile.getLoaders(), input, fileIdArg
    )
}

var overrideYes = false

fun ynPrompt(prompt: String, terminal: Terminal, default: Boolean? = null): Boolean
{
    return if (overrideYes) true else YesNoPrompt(prompt, terminal, default).ask() == true
}
