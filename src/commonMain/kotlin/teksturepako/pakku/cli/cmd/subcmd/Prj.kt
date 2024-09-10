package teksturepako.pakku.cli.cmd.subcmd

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.ActionError.NotFoundOnPlatform
import teksturepako.pakku.api.actions.ActionError.ProjNotFound
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.cli.arg.splitProjectArg
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.*

class Prj : CliktCommand()
{
    override fun help(context: Context) = "Specify the project precisely"

    private val cfOpt by option(
        "--cf", "--curseforge", help = "CurseForge project slug or ID"
    )
    private val mrOpt by option(
        "--mr", "--modrinth", help = "Modrinth project slug or ID"
    )

    private val flags by requireObject<Map<String, Boolean>>()

    override fun run(): Unit = runBlocking {
        val noDepsFlag = flags.getOrElse("noDepsFlag") { false }

        val lockFile = LockFile.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        val projectProvider = lockFile.getProjectProvider().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        suspend fun add(projectIn: Project?, strict: Boolean = true)
        {
            suspend fun handleMissingProject(error: NotFoundOnPlatform)
            {
                val prompt = promptForProject(error.platform, terminal, lockFile)
                    ?: return add(projectIn, strict = false)

                val (promptedProject, promptedArgs) = prompt
                if (promptedProject == null) return terminal.pError(ProjNotFound(), promptedArgs.first)

                (error.project + promptedProject).fold( // Combine projects
                    failure = { terminal.pError(it) },
                    success = { add(it) }
                )
            }

            projectIn.createAdditionRequest(
                onError = { error ->
                    terminal.pError(error)

                    if (error is NotFoundOnPlatform && strict)
                    {
                        handleMissingProject(error)
                    }
                },
                onSuccess = { project, isRecommended, reqHandlers ->
                    val projMsg = project.getFullMsg()

                    if (ynPrompt("Do you want to add $projMsg?", terminal, isRecommended))
                    {
                        lockFile.add(project)
                        lockFile.linkProjectToDependents(project)

                        if (!noDepsFlag)
                        {
                            project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        }

                        terminal.pSuccess("$projMsg added")
                    }
                },
                lockFile, platforms, strict
            )
        }

        val cf = cfOpt?.let {
            val (input, fileId) = splitProjectArg(it)

            CurseForge.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), input, fileId
            )
        }

        val mr = mrOpt?.let {
            val (input, fileId) = splitProjectArg(it)

            Modrinth.requestProjectWithFiles(
                lockFile.getMcVersions(), lockFile.getLoaders(), input, fileId
            )
        }

        // Combine projects or return just one of them.
        val comb = cf?.let { c ->
            mr?.let { m ->
                (c + m).get() // Combine projects if project is available from both platforms.
            } ?: c // Return the CurseForge project if Modrinth project is missing.
        } ?: mr // Return the Modrinth project if CurseForge project is missing.

        add(comb)
        echo()

        lockFile.write()?.let { terminal.pError(it) }
    }
}