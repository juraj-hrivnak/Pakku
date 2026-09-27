package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.get
import kotlinx.coroutines.test.runTest
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.api.projects.UpdateStrategy
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertNotNull

class CfgPrjTest : PakkuTest()
{
    @Test
    fun `should fail without lock file`()
    {
        val cmd = CfgPrj()
        val output = cmd.test("test --subpath test-subpath").output

        assert(FileNotFound(Path(workingPath, LockFile.FILE_NAME).pathString).rawMessage in output)
    }

    @Test
    fun `should success with lock file & project`() = runTest {
        val lockFile = LockFile.readOrNew().get()!!
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

        val cmd = CfgPrj()
        val output = cmd.test("test -p test -s both -u latest -r true")

        assert(output.stderr == "") { "Command failed to execute" }
        assertNotNull(ConfigFile.readOrNull(), "Config file should be created")

        val config = ConfigFile.readOrNull()!!.projects["test"]

        assertNotNull(config, "Project config should be created")
        assert(config.updateStrategy == UpdateStrategy.LATEST)
        assert(config.redistributable == true)
        assert(config.subpath == "test")
        assert(config.side == ProjectSide.BOTH)
    }
}
