package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.terminal.prompt
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.createRemovalRequest
import teksturepako.pakku.api.actions.sync.syncProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.ProjectOrigin
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.promptForCurseForgeApiKey
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.*

class Sync : CliktCommand()
{
    override fun help(context: Context) = "Sync your modpack with local project files"

    private val additionsFlag by option("-A", "--additions").flag()
        .help("Sync additions only")

    private val removalsFlag by option("-R", "--removals").flag()
        .help("Sync removals only")

    private val updatesFlag by option("-U", "--updates").flag()
        .help("Sync updates only")

    private val fromParentFlag by option("--from-parent").flag()
        .help("Sync changes from parent modpack")

    private val preferUpstreamOpt by option("--prefer-upstream").flag()
        .help("On conflict, prefer upstream version over local-only")

    private val preferLocalOpt by option("--prefer-local").flag()
        .help("On conflict, prefer local-only version over upstream")

    override fun run(): Unit = runBlocking {
        val lockFile = LockFile.readToResult().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val configFile = if (ConfigFile.exists())
        {
            ConfigFile.readToResult().getOrElse {
                terminal.pError(it)
                echo()
                return@runBlocking
            }
        }
        else null

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val flagsUsed = additionsFlag || removalsFlag || updatesFlag

        // -- FROM PARENT SYNC --
        if (fromParentFlag)
        {
            val config = configFile ?: run {
                echo("No pakku.json found. Run 'pakku parent set' first.")
                throw ProgramResult(1)
            }

            val parentConfig = config.getParent() ?: run {
                echo("No parent modpack configured. Run 'pakku parent set' first.")
                throw ProgramResult(1)
            }

            echo("Fetching parent modpack...")
            val parentLockFile = LockFile.readFromParent(parentConfig).getOrElse { error ->
                terminal.pError(error)
                throw ProgramResult(1)
            }

            val localProjects = lockFile.getAllProjects()
            val parentProjects = parentLockFile.getAllProjects()

            val localSlugs = localProjects.map { it.pakkuId!! }.toSet()
            val parentSlugs = parentProjects.map { it.pakkuId!! }.toSet()

            val upstreamMods = localProjects.filter { it.pakkuId in parentSlugs }
            val localOnly = localProjects.filter { it.pakkuId !in parentSlugs }

            echo()
            echo("Divergence Report:")
            echo("  Upstream mods: ${upstreamMods.size}")
            echo("  Local additions: ${localOnly.size}")
            echo()

            val parentSlugsOnly = parentSlugs - localSlugs
            val removedFromUpstream = lockFile.getProjectsByOrigin(ProjectOrigin.UPSTREAM).filter { it.pakkuId !in parentSlugs }

            var changed = false

            // Detect conflicts: parent adds project that exists as local-only
            val conflicts = parentSlugsOnly.mapNotNull { slug ->
                val parentProject = parentProjects.find { it.pakkuId == slug }!!
                val existingLocal = localOnly.find { it.pakkuId == slug }
                if (existingLocal != null && existingLocal.slug.values.any { configFile.isLocalOnly(it) })
                {
                    parentProject to existingLocal
                } else null
            }

            // Handle conflicts
            if (conflicts.isNotEmpty())
            {
                echo("⚠️  Conflicts detected (${conflicts.size}):")
                echo("Parent has added projects that already exist as local-only in your modpack.")
                echo()

                val resolvedConflicts = mutableSetOf<String>()

                for ((parentProject, localProject) in conflicts)
                {
                    echo("Conflict: ${parentProject.getFullMsg()}")
                    echo("  Upstream: ${parentProject.files.firstOrNull()?.fileName}")
                    echo("  Local:    ${localProject.files.firstOrNull()?.fileName}")
                    echo()

                    val choice = when
                    {
                        preferUpstreamOpt -> 2
                        preferLocalOpt -> 1
                        else ->
                        {
                            echo("Choose action:")
                            echo("  [1] Keep as local-only (ignore upstream version)")
                            echo("  [2] Switch to upstream version (remove from local-only)")
                            echo("  [3] Skip (decide later)")
                            terminal.prompt("Enter choice (1-3)")?.toIntOrNull() ?: 3
                        }
                    }

                    when (choice)
                    {
                        1 ->
                        {
                            terminal.pInfo("Keeping ${localProject.getFullMsg()} as local-only")
                            resolvedConflicts.add(localProject.pakkuId!!)
                        }
                        2 ->
                        {
                            lockFile.update(parentProject)
                            lockFile.setProjectOrigin(parentProject.pakkuId!!, ProjectOrigin.UPSTREAM)
                            localProject.slug.values.forEach { configFile.removeLocalOnly(it) }
                            terminal.pSuccess("Switched ${parentProject.getFullMsg()} to upstream version")
                            resolvedConflicts.add(parentProject.pakkuId!!)
                            changed = true
                        }
                        else ->
                        {
                            terminal.pInfo("Skipped ${localProject.getFullMsg()}")
                            resolvedConflicts.add(localProject.pakkuId!!)
                        }
                    }
                    echo()
                }

                // Filter out conflicts from parentSlugsOnly
                val parentSlugsOnlyFiltered = parentSlugsOnly.filter { it !in resolvedConflicts }

                // Add new mods from parent (non-conflicting)
                if (parentSlugsOnlyFiltered.isNotEmpty())
                {
                    echo("New mods in parent (${parentSlugsOnlyFiltered.size}):")
                    for (slug in parentSlugsOnlyFiltered)
                    {
                        val parentProject = parentProjects.find { it.pakkuId == slug }!!
                        echo("  + ${parentProject.getFullMsg()}")
                    }
                    echo()

                    if (terminal.ynPrompt("Add these ${parentSlugsOnlyFiltered.size} mods to your modpack?", false))
                    {
                        for (slug in parentSlugsOnlyFiltered)
                        {
                            val parentProject = parentProjects.find { it.pakkuId == slug }!!
                            lockFile.add(parentProject)
                            lockFile.setProjectOrigin(parentProject.pakkuId!!, ProjectOrigin.UPSTREAM)
                            changed = true
                        }
                        terminal.pSuccess("Added ${parentSlugsOnlyFiltered.size} mods from parent")
                    }
                }
            }
            else
            {
                // No conflicts, add new mods from parent
                if (parentSlugsOnly.isNotEmpty())
                {
                    echo("New mods in parent (${parentSlugsOnly.size}):")
                    for (slug in parentSlugsOnly)
                    {
                        val parentProject = parentProjects.find { it.pakkuId == slug }!!
                        echo("  + ${parentProject.getFullMsg()}")
                    }
                    echo()

                if (terminal.ynPrompt("Add these ${parentSlugsOnly.size} mods to your modpack?", false))
                {
                    for (slug in parentSlugsOnly)
                    {
                        val parentProject = parentProjects.find { it.pakkuId == slug }!!
                        lockFile.add(parentProject)
                        lockFile.setProjectOrigin(parentProject.pakkuId!!, ProjectOrigin.UPSTREAM)
                        changed = true
                    }
                    terminal.pSuccess("Added ${parentSlugsOnly.size} mods from parent")
                }
                }
            }

            // Update upstream mods
            val upstreamToUpdate = mutableListOf<Pair<Project, Project>>()
            for (local in upstreamMods)
            {
                val parent = parentProjects.find { it.pakkuId == local.pakkuId }!!
                if (local.files.firstOrNull()?.id != parent.files.firstOrNull()?.id)
                {
                    upstreamToUpdate.add(local to parent)
                }
            }

            if (upstreamToUpdate.isNotEmpty())
            {
                echo("Updates in parent (${upstreamToUpdate.size}):")
                for ((local, parent) in upstreamToUpdate)
                {
                    echo("  ~ ${local.getFullMsg()}")
                    echo("    ${local.files.firstOrNull()?.fileName} -> ${parent.files.firstOrNull()?.fileName}")
                }
                echo()

                if (terminal.ynPrompt("Update ${upstreamToUpdate.size} mods from parent?", false))
                {
                    for ((_, parent) in upstreamToUpdate)
                    {
                        lockFile.update(parent)
                        lockFile.setProjectOrigin(parent.pakkuId!!, ProjectOrigin.UPSTREAM)
                        changed = true
                    }
                    terminal.pSuccess("Updated ${upstreamToUpdate.size} mods from parent")
                }
            }

            // Handle removed from upstream
            if (removedFromUpstream.isNotEmpty())
            {
                echo("Mods removed from parent (${removedFromUpstream.size}):")
                for (project in removedFromUpstream)
                {
                    echo("  - ${project.getFullMsg()}")
                }
                echo()

                if (terminal.ynPrompt("Remove ${removedFromUpstream.size} mods from your modpack?", false))
                {
                    for (project in removedFromUpstream)
                    {
                        lockFile.remove(project)
                        lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                        changed = true
                    }
                    terminal.pSuccess("Removed ${removedFromUpstream.size} mods")
                }
            }

            if (!changed)
            {
                echo("No changes needed from parent.")
            }

            lockFile.write()?.onError { error ->
                terminal.pError(error)
                throw ProgramResult(1)
            }
            return@runBlocking
        }

        val progressBar = progressBarLayout(spacing = 2) {
            spinner(Spinner.Dots())
        }.animateInCoroutine(terminal)

        launch { progressBar.execute() }

        val (addedProjects, removedProjects, updatedProjects) = syncProjects(
            onError = { terminal.pError(it ) },
            lockFile, configFile, platforms
        )

        progressBar.clear()

        // -- ADDITIONS --

        if (!flagsUsed || additionsFlag)
        {
            addedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = addedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (addedProjects.size > 1) "were" else "was"

                echo("$msg $verb added to your modpack's file system.")
                echo()
            }

            for (projectIn in addedProjects)
            {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        terminal.pError(error)

                        if (error is CurseForge.Unauthenticated)
                        {
                            terminal.promptForCurseForgeApiKey()?.onError { terminal.pError(it) }
                        }
                    },
                    onSuccess = { project, isRecommended, replacing, _ ->
                        val projMsg = project.getFullMsg()
                        val promptMessage = if (replacing == null)
                        {
                            "Do you want to add $projMsg?" to "$projMsg added"
                        }
                        else
                        {
                            val replacingMsg = replacing.getFullMsg()
                            "Do you want to replace $replacingMsg with $projMsg?" to
                                    "$replacingMsg replaced with $projMsg"
                        }

                        if (terminal.ynPrompt(promptMessage.first, isRecommended))
                        {
                            if (replacing == null) lockFile.add(project) else lockFile.update(project)
                            lockFile.linkProjectToDependents(project)

                            terminal.pSuccess(promptMessage.second)
                        }
                    },
                    lockFile, platforms
                )
            }

            if (addedProjects.isNotEmpty()) echo()
        }

        // -- REMOVALS --

        if (!flagsUsed || removalsFlag)
        {
            removedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = removedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (removedProjects.size > 1) "were" else "was"

                echo("$msg $verb removed from your modpack's file system.")
                echo()
            }

            for (projectIn in removedProjects)
            {
                projectIn.createRemovalRequest(
                    onError = { error ->
                        terminal.pError(error)
                    },
                    onRemoval = { project, isRecommended ->
                        val projMsg = project.getFullMsg()

                        if (terminal.ynPrompt("Do you want to remove $projMsg?", isRecommended))
                        {
                            lockFile.remove(project)
                            lockFile.removePakkuLinkFromAllProjects(project.pakkuId!!)
                            terminal.pDanger("$projMsg removed")

                            project.getSubpath()?.onSuccess {
                                configFile?.setProjectConfig(projectIn, lockFile) { slug ->
                                    this.subpath = null
                                    terminal.pDanger("'projects.$slug.subpath' removed")
                                }
                            }?.onFailure { error ->
                                terminal.pError(error)
                            }
                        }
                    },
                    onDepRemoval = { _, _ ->

                    },
                    lockFile
                )
            }

            if (removedProjects.isNotEmpty()) echo()
        }

        // -- UPDATES -

        if (!flagsUsed || updatesFlag)
        {
            updatedProjects.takeIf { it.isNotEmpty() }?.run {
                val msg = updatedProjects.map { it.getFullMsg() }.toMsg()
                val verb = if (updatedProjects.size > 1) "were" else "was"

                echo("$msg $verb updated in your modpack's file system.")
                echo()
            }

            for (project in updatedProjects)
            {
                if (terminal.ynPrompt("Do you want to update ${project.getFullMsg()}?", true))
                {
                    lockFile.update(project)
                    terminal.pInfo("${project.getFullMsg()} updated")

                    project.getSubpath()?.onSuccess { subpath ->
                        configFile?.setProjectConfig(project, lockFile) { slug ->
                            this.subpath = subpath
                            terminal.pInfo("'projects.$slug.subpath' set to '$subpath'")
                        }
                    }?.onFailure { error ->
                        terminal.pError(error)
                    }
                }
            }

            if (updatedProjects.isNotEmpty()) echo()
        }

        lockFile.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
        configFile?.write()?.onError { error ->
            terminal.pError(error)
            throw ProgramResult(1)
        }
    }
}
