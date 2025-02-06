package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.import.importModpackModel
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.cli.resolveDependencies
import teksturepako.pakku.cli.ui.getFullMsg
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pSuccess
import java.nio.file.Path
import kotlin.io.path.pathString

class Import : CliktCommand()
{
    override fun help(context: Context) = "Import modpack"

    private val pathArg: Path by argument("path", help = "The path to the modpack file").path(mustExist = true)
    private val depsFlag: Boolean by option("-D", "--deps", help = "Resolve dependencies").flag()

    override fun run() = runBlocking {
        val modpackModel = importModpackModel(pathArg).getOrElse {
            terminal.pError(it, pathArg.pathString)
            echo()
            return@runBlocking
        }

        val lockFile = LockFile.readToResult().get() ?: modpackModel.toLockFile()

        val platforms: List<Platform> = lockFile.getPlatforms().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val projectProvider = lockFile.getProjectProvider().getOrElse {
            terminal.pError(it)
            echo()
            return@runBlocking
        }

        val importedProjects = modpackModel.toSetOfProjects(lockFile, platforms)

        importedProjects.map { projectIn ->
            launch {
                projectIn.createAdditionRequest(
                    onError = { error ->
                        if (error !is AlreadyAdded) terminal.pError(error)
                    },
                    onSuccess = { project, _, replacing, reqHandlers ->
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

                        if (replacing == null) lockFile.add(project) else lockFile.update(project)
                        lockFile.linkProjectToDependents(project)

                        if (depsFlag)
                        {
                            project.resolveDependencies(terminal, reqHandlers, lockFile, projectProvider, platforms)
                        }

                        terminal.pSuccess("${project.getFullMsg()} ${promptMessage.second}")
                        Modrinth.checkRateLimit()
                    },
                    lockFile, platforms
                )
            }
        }.joinAll()

        lockFile.write()
    }
}