package teksturepako.pakku.integration.git

import com.github.michaelbull.result.runCatching
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Duration

fun pakkuGitProgressMonitor(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): Triple<TextProgressMonitor, ByteArrayOutputStream, BufferedWriter>
{
    val outputStream = ByteArrayOutputStream()
    val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

    val progressMonitor = object : TextProgressMonitor(writer)
    {
        override fun onUpdate(taskName: String?, workCurr: Int, workTotal: Int, percentDone: Int, duration: Duration?)
        {
            super.onUpdate(taskName, workCurr, workTotal, percentDone, duration)
            runCatching { writer.flush() }
            onProgress(taskName, percentDone)
            outputStream.reset()
        }
    }

    return Triple(progressMonitor, outputStream, writer)
}
