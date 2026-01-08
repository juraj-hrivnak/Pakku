package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.ParentConfig
import teksturepako.pakku.api.data.ProjectOrigin
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import java.nio.file.Path

class Parent : CliktCommand()
{
    override fun help(context: Context) = "Manage parent modpack for divergent forks"

    private val typeOpt by option("-t", "--type")
        .help("Platform type: cf (CurseForge), mr (Modrinth), local")

    private val versionOpt by option("-v", "--version")
        .help("Specific version to link to (optional, defaults to latest)")

    private val unsetFlag by option("--unset").flag()
        .help("Remove parent link")

    private val keepProjectsFlag by option("--keep-projects").flag()
        .help("When unsetting parent, convert all upstream projects to local (retain all projects)")

    private val showFlag by option("--show").flag()
        .help("Show current parent configuration")

    private val sourceArg by argument(help = "Source (CF project ID, Modrinth slug, or local path)")

    override fun run(): Unit = runBlocking {
        when {
            showFlag -> showParent()
            unsetFlag -> unsetParent()
            else -> setParent()
        }
    }

    private suspend fun showParent()
    {
        val config = ConfigFile.readToResult().getOrElse {
            terminal.pError(it)
            throw ProgramResult(1)
        }

        val parent = config.getParent()
        if (parent == null)
        {
            echo("No parent modpack configured")
            return
        }

        val lockFile = LockFile.readOrNew()

        val upstreamCount = lockFile.getUpstreamProjectCount()
        val localCount = lockFile.getLocalProjectCount()
        val totalCount = lockFile.getAllProjects().size

        echo("Parent modpack:")
        echo("  Type: ${parent.type}")
        echo("  ID: ${parent.id}")
        parent.version?.let { echo("  Version: $it (pinned)") } ?: echo("  Version: tracking latest")
        echo("  Auto-sync: ${if (parent.autoSync) "enabled" else "disabled"}")
        echo("")
        echo("Project breakdown:")
        echo("  Upstream projects: $upstreamCount")
        echo("  Local additions: $localCount")
        echo("  Total: $totalCount")
    }

    private suspend fun setParent()
    {
        val source = sourceArg.trim()
        val type = typeOpt?.lowercase() ?: detectType(source)

        if (type == null)
        {
            echo("Could not detect platform type. Use --type to specify (cf, mr, or local)")
            throw ProgramResult(1)
        }

        val parentConfig = ParentConfig(
            type = type,
            id = source,
            version = versionOpt,
            autoSync = false
        )

        val config = ConfigFile.readToResult().getOrElse { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }

        config.setParent(parentConfig)
        config.write()?.let { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }

        terminal.pSuccess("Parent modpack set:")
        echo("  Type: $type")
        echo("  ID: $source")
        versionOpt?.let { echo("  Version: $it") }
    }

    private suspend fun unsetParent()
    {
        val config = ConfigFile.readToResult().getOrElse { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }

        if (!config.hasParent())
        {
            echo("No parent modpack configured")
            return
        }

        if (keepProjectsFlag)
        {
            val lockFile = LockFile.readOrNew()
            
            // Convert all UPSTREAM projects to LOCAL
            val upstreamProjects = lockFile.getProjectsByOrigin(ProjectOrigin.UPSTREAM)
            upstreamProjects.forEach { project ->
                lockFile.setProjectOrigin(project.pakkuId!!, ProjectOrigin.LOCAL)
            }
            
            // Clear localOnly list since everything is now local
            config.clearLocalOnly()
            
            // Write lock file
            lockFile.write()?.let { error ->
                terminal.pError(error)
                throw ProgramResult(1)
            }
            
            terminal.pSuccess("Converted ${upstreamProjects.size} upstream projects to local")
            echo("Your modpack is now independent. All ${lockFile.getAllProjects().size} projects retained.")
        }

        config.setParent(null)
        config.write()?.let { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }

        terminal.pSuccess("Parent link removed")
    }

    private fun detectType(source: String): String?
    {
        // Local path check
        val path = Path.of(source)
        if (path.toFile().exists() || path.toFile().isDirectory)
        {
            return "local"
        }

        // CurseForge: numeric ID
        if (source.matches(Regex("^\\d+$")))
        {
            return "curseforge"
        }

        // Modrinth: slug format (alphanumeric, dashes)
        if (source.matches(Regex("^[a-z0-9-]+$")))
        {
            return "modrinth"
        }

        return null
    }
}
