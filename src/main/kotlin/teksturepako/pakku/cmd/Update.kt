package teksturepako.pakku.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.platforms.Multiplatform

class Update : CliktCommand()
{
    private val mods: List<String> by argument().multiple()
    private val all: Boolean by option("-a", "--all", help = "Remove all mods").flag()

    override fun run() = runBlocking {
        if (all)
        {
            TODO()
        }
        else for (deferred in mods.map { arg ->
            async {
                Multiplatform.requestProjectFiles(PakkuLock.getMcVersions(), PakkuLock.getLoaders(), arg) to arg
            }
        })
        {
            val (project, arg) = deferred.await()
        }
    }
}