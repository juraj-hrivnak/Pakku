package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.prefixed
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class Run : CliktCommand("Run your modpack instance")
{
    override fun run(): Unit = runBlocking {

        val multiMcPath = listOf(
            Path(workingPath, "..", "..", "..", "MultiMC.exe"),
            Path(workingPath, "..", "..", "..", "MultiMC")
        ).find { it.exists() }?.absolute() ?: return@runBlocking terminal.danger("Did not find MultiMC.")

        if (!multiMcPath.isExecutable()) return@runBlocking terminal.danger("MultiMC is not executable.")

        val multiMcProfilePath = Path(workingPath, "..", "instance.cfg")
            .takeIf { it.exists() }
            ?.absolute() ?: return@runBlocking terminal.danger("Did not find 'instance.cfg'.")

        val multiMcInstance = multiMcProfilePath.readLines()
            .find { it.startsWith("name=") }
            ?.substringAfter("name=") ?: return@runBlocking terminal.danger("Failed to parse 'instance.cfg'.")

        terminal.success(prefixed("Starting MultiMC instance: '$multiMcInstance'"))

        launch {
            process(multiMcPath.toString(), "-l", multiMcInstance, stdout = Redirect.SILENT, stderr = Redirect.SILENT)
        }.join()

        echo()

        terminal.success(prefixed("Stopped MultiMC instance: '$multiMcInstance'"))

        echo()
    }
}
