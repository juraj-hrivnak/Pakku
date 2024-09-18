package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.cli.arg.splitGitHubProjectArg
import teksturepako.pakku.toPrettyString

class Gh : CliktCommand()
{
    private val projectArgs: List<String> by argument("projects", help = "Projects to add").multiple()

    override fun run(): Unit = runBlocking {
        projectArgs.mapNotNull x@ { arg ->
            val (owner, repo) = splitGitHubProjectArg(arg) ?: return@x null

            val prj = GitHub.requestProjectWithFiles(listOf(), listOf(),"$owner/$repo") ?: return@x null

            terminal.println(prj.toPrettyString())
        }
    }

}