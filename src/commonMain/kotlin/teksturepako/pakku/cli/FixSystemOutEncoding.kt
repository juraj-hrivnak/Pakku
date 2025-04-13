package teksturepako.pakku.cli

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Kernel32Util
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Fixes the system output encoding on Windows systems to properly handle UTF-8 characters.
 *
 * This function attempts to configure the Windows console to use UTF-8 encoding (code page 65001)
 * and sets up the system output streams ([System.out] and [System.err]) to use UTF-8 encoding.
 *
 * @return `true` if the encoding was successfully fixed or if no fix was needed
 *         (non-Windows systems or no console), `false` if the operation failed
 */
fun fixSystemOutEncoding(): Boolean
{
    if (System.console() == null || !System.getProperty("os.name").lowercase().contains("win")) return true

    try
    {
        // Set console code page to 65001 = UTF-8
        if (Kernel32.INSTANCE.SetConsoleOutputCP(65001))
        {
            // Replace System.out and System.err with PrintStreams using UTF-8
            System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8))
            System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8))
        }
        else
        {
            // SetConsoleOutputCP() failed, throw exception with error message.
            throw RuntimeException(Kernel32Util.getLastErrorMessage())
        }
    }
    catch (t: Throwable)
    {
        return false
    }

    return true
}