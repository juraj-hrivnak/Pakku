package teksturepako.pakku.cli

import com.github.ajalt.mordant.terminal.StringPrompt
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

fun promptForProject(platform: Platform, terminal: Terminal): Project? = runBlocking {
    val prompt = StringPrompt("Specify ${platform.name}", terminal).ask()

    if (prompt.isNullOrBlank()) return@runBlocking null

    return@runBlocking Multiplatform.requestProjectWithFiles(
        PakkuLock.getMcVersions(), PakkuLock.getLoaders(), prompt
    )
}