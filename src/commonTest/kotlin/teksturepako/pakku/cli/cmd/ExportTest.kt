package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains

class ExportTest : PakkuTest()
{
    private val testName = "TestModpack"
    private val testVersion = "1.0.0"
    private val testDescription = "This is a test modpack."
    private val testAuthor = "TestAuthor"

    @Test
    fun `try without lock file & config file`()
    {
        val cmd = Export()
        val output = cmd.test().output

        assertContains(output, "Could not read '$workingPath/${LockFile.FILE_NAME}'")
    }
}