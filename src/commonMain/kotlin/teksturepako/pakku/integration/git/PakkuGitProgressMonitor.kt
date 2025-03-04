package teksturepako.pakku.integration.git

import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import org.eclipse.jgit.lib.TextProgressMonitor
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

fun pakkuGitProgressMonitor(
    onProgress: (taskName: String?, percentDone: Int) -> Unit,
): Triple<TextProgressMonitor?, ByteArrayOutputStream, BufferedWriter>
{
    val outputStream = ByteArrayOutputStream()
    val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))

    val progressMonitor = runCatching {
        object : TextProgressMonitor(writer)
        {
            override fun onUpdate(taskName: String?, cmp: Int, totalWork: Int, pcnt: Int)
            {
                super.onUpdate(taskName, cmp, totalWork, pcnt)
                runCatching { writer.flush() }
                onProgress(taskName, pcnt)
                outputStream.reset()
            }
        }
    }.get() ?: return Triple(null, outputStream, writer)

    return Triple(progressMonitor, outputStream, writer)
}
