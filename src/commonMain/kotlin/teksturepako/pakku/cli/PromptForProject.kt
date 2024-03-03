package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

suspend fun promptForProject(platform: Platform, terminal: Terminal, lockFile: LockFile): Project?
{
    val prompt = StringPrompt("Specify ${platform.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return null

    return Multiplatform.requestProjectWithFiles(
        lockFile.getMcVersions(), lockFile.getLoaders(), prompt
    )
}