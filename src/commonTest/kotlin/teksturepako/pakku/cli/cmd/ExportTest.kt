package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains

@OptIn(ExperimentalPathApi::class)
class ExportTest
{
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

    @Test
    fun `try with lock file & config file`() = runBlocking {
        LockFile(
            "multiplatform",
            mutableListOf("1.20.1"),
            mutableMapOf("forge" to "")
        ).write()

        ConfigFile(
            "TestModpack",
            "1.0.0",
            "This is a test modpack.",
            "TestAuthor"
        ).write()

        val cmd = Export()
        cmd.test().output

        assert(Path(workingPath, "TestModpack-1.0.0.zip").exists())
        assert(Path(workingPath, "TestModpack-1.0.0.mrpack").exists())
    }
}