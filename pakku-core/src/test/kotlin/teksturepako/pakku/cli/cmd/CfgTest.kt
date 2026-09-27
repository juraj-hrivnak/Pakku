package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.ProjectType
import kotlin.test.Test
import kotlin.test.assertNotNull

class CfgTest : PakkuTest()
{
    @Test
    fun `should success with options`()
    {
        val cmd = Cfg()
        val output = cmd.test("-n foo -v 1.20.1 -d bar -a test --mods-path ./dummy-mods --resource-packs-path ./dummy-resourcepacks --data-packs-path ./datapacks --worlds-path ./worlds --shaders-path ./shaders")
        assert(output.stderr == "") { "Command failed to execute" }
        val config = ConfigFile.readOrNull()
        assertNotNull(config, "Config file should be created")
        assert(config.getName() == "foo")
        assert(config.getVersion() == "1.20.1")
        assert(config.getDescription() == "bar")
        assert(config.getAuthor() == "test")
        assert(config.paths[ProjectType.MOD.serialName] == "./dummy-mods")
        assert(config.paths[ProjectType.RESOURCE_PACK.serialName] == "./dummy-resourcepacks")
        assert(config.paths[ProjectType.DATA_PACK.serialName] == "./datapacks")
        assert(config.paths[ProjectType.WORLD.serialName] == "./worlds")
        assert(config.paths[ProjectType.SHADER.serialName] == "./shaders")
    }
}
