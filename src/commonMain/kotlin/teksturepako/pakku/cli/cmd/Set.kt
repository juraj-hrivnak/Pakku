package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.success
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.*
import teksturepako.pakku.api.data.ProjectOrigin
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.ui.pError

class Set : CliktCommand()
{
    override val printHelpOnEmptyArgs = true

    override fun help(context: Context) = "Set properties of the lock file or projects"

    // -- PACK --

    private val targetOpt: String? by option(
        "-t", "--target",
        help = "Change the target of the pack"
    ).choice("curseforge", "modrinth", "multiplatform", ignoreCase = true)

    private val mcVersionsOpts: List<String>? by option(
        "-v", "--mc-versions",
        help = "Change the minecraft versions"
    ).varargValues()

    private val loadersOpts: Map<String, String>? by option(
        "-l", "--loaders",
        help = "Change the mod loaders",
        metavar = "<name>=<version>"
    ).associate()

    // -- PROJECTS --

    private val projectArgs: List<String> by argument(
        "projects",
        help = "Project slugs to modify"
    ).multiple(required = false)

    private val localOnlyFlag by option(
        "--local-only",
        help = "Mark project(s) as local-only (won't sync from parent)"
    ).flag(default = false)

    private val noLocalOnlyFlag by option(
        "--no-local-only",
        help = "Unmark project(s) as local-only (will sync from parent)"
    ).flag(default = false)

    override fun run() = runBlocking {
        val lockFile = LockFile.readOrNew()

        // -- PACK --

        /* Target */
        targetOpt?.let {
            lockFile.setTarget(it)
            terminal.success("'target' set to '$it'")
        }

        /* Minecraft versions */
        mcVersionsOpts?.let { versions ->
            var failed = false

            lockFile.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.mcVersions.isNotEmpty() && file.mcVersions.none { it in versions })
                    {
                        terminal.danger(
                            "Can not set to $versions,"
                            + " because ${project.name.values.first()} (${file.type}) requires ${file.mcVersions}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                lockFile.setMcVersions(versions)
                terminal.success("'mc_version' set to $versions")
            }
        }

        /* Loaders */
        loadersOpts?.let { loaders ->
            if (loadersOpts.isNullOrEmpty()) return@let

            var failed = false

            lockFile.getAllProjects().forEach { project ->
                for (file in project.files)
                {
                    if (file.loaders.isNotEmpty() && file.loaders.none { it in loaders || it in Platform.validLoaders })
                    {
                        terminal.danger(
                            "Can not set to $loaders,"
                            + " because ${project.name.values.first()} (${file.type}) requires ${file.loaders}"
                        )
                        failed = true
                    }
                }
            }

            if (!failed)
            {
                lockFile.setLoaders(loaders)
                terminal.success("'loaders' set to $loaders")
            }
        }

        // -- PROJECTS --

        if (localOnlyFlag && noLocalOnlyFlag)
        {
            terminal.danger("Cannot use both --local-only and --no-local-only flags")
            throw ProgramResult(1)
        }

        if ((localOnlyFlag || noLocalOnlyFlag) && projectArgs.isEmpty())
        {
            terminal.danger("Must specify project(s) when using --local-only or --no-local-only")
            throw ProgramResult(1)
        }

        if (localOnlyFlag || noLocalOnlyFlag)
        {
            val configFile = ConfigFile.readOrNew()

            projectArgs.forEach { slug ->
                val project = lockFile.getAllProjects().find { 
                    it.slug.values.any { it.equals(slug, ignoreCase = true) }
                }

                if (project == null)
                {
                    terminal.danger("Project '$slug' not found in lock file")
                    return@runBlocking
                }

                if (localOnlyFlag)
                {
                    lockFile.setProjectOrigin(project.pakkuId!!, ProjectOrigin.LOCAL)
                    configFile.addLocalOnly(project.slug.values.first())
                    terminal.success("'${project.slug.values.first()}' marked as local-only")
                }
                else if (noLocalOnlyFlag)
                {
                    lockFile.setProjectOrigin(project.pakkuId!!, ProjectOrigin.UPSTREAM)
                    configFile.removeLocalOnly(project.slug.values.first())
                    terminal.success("'${project.slug.values.first()}' unmarked as local-only")
                }
            }

            configFile.write()?.onError { error ->
                terminal.pError(error)
                throw ProgramResult(1)
            }
        }

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
        echo()
    }
}