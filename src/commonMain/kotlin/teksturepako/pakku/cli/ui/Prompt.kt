package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.YesNoPrompt
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.IProjectProvider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.splitProjectArg

suspend fun promptForProject(
    provider: IProjectProvider, terminal: Terminal, lockFile: LockFile, fileId: String? = null
): Pair<Project?, Triple<String, String, String?>>?
{
    val prompt = StringPrompt("Specify ${provider.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return null

    val (input, fileIdArg) = splitProjectArg(prompt)

    return provider.requestProjectWithFiles(
        lockFile.getMcVersions(), lockFile.getLoaders(), input, fileIdArg ?: fileId
    ) to Triple(prompt, input, fileIdArg)
}

var overrideYes = false

fun ynPrompt(prompt: String, terminal: Terminal, default: Boolean? = null): Boolean
{
    return if (overrideYes) true else YesNoPrompt(prompt, terminal, default).ask() == true
}
