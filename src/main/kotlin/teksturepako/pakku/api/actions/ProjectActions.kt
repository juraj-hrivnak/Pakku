package teksturepako.pakku.api.actions

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

object ProjectActions
{
    suspend fun promptForProject(terminal: Terminal, platform: Platform): Project?
    {
        val prompt = StringPrompt("Specify ${platform.name}", terminal).ask()

        if (prompt.isNullOrBlank()) return null

        return Multiplatform.requestProjectFiles(
            PakkuLock.getMcVersions(), PakkuLock.getLoaders(), prompt
        )
    }
}