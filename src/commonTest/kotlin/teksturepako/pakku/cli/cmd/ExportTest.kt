package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains

@OptIn(ExperimentalPathApi::class)
class ExportTest
{
    private val testName = "TestModpack"
    private val testVersion = "1.0.0"
    private val testDescription = "This is a test modpack."
    private val testAuthor = "TestAuthor"

    init
    {
        workingPath = "./build/test"
        runCatching { Path("./build/test").deleteRecursively() }
        runCatching { Path("./build/test").createDirectory() }
    }

    @Test
    fun `try without lock file & config file`()
    {
        val cmd = Export()
        val output = cmd.test().output

        assertContains(output, "Could not read '$workingPath/${LockFile.FILE_NAME}'")
    }
}