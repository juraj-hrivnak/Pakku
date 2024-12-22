package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CfgPrjTest : PakkuTest()
{
    @Test
    fun `should fail without lock file`()
    {
        val cmd = CfgPrj()
        val output = cmd.test("test --subpath test-subpath").output

        assertContains(output, "Could not read '$workingPath/${LockFile.FILE_NAME}'")
    }

    @Test
    fun `should success with lock file & project`()
    {
        runBlocking {
            val lockFile = LockFile.readOrNew()
            lockFile.add(
                Project(
                    type = ProjectType.MOD,
                    slug = mutableMapOf("modrinth" to "test"),
                    name = mutableMapOf("modrinth" to "Test"),
                    id = mutableMapOf("modrinth" to "test"),
                    files = mutableSetOf()
                )
            )
            lockFile.write()
        }

        val cmd = CfgPrj()
        val output = cmd.test("test -p test -s both -u latest -r true")

        assertEquals("", output.stderr, "Command failed to execute")
        assertNotNull(ConfigFile.readOrNull(), "Config file should be created")

        val config = ConfigFile.readOrNull()!!.projects["test"]

        assertNotNull(config, "Project config should be created")
        assertEquals(UpdateStrategy.LATEST, config.updateStrategy)
        assertEquals(true, config.redistributable)
        assertEquals("test", config.subpath)
        assertEquals(ProjectSide.BOTH, config.side)
    }
}
