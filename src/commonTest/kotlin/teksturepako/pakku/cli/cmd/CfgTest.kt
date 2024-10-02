package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.ProjectType
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalPathApi::class)
class CfgTest
{
    init
    {
        workingPath = "./build/test"
        runCatching { Path("./build/test").deleteRecursively() }
        runCatching { Path("./build/test").createDirectory() }
    }

    @Test
    fun `should success with options`()
    {
        val cmd = Cfg()
        val output =
            cmd.test("-n foo -v 1.20.1 -d bar -a test --mods-path ./dummy-mods --resource-packs-path ./dummy-resourcepacks --data-packs-path ./datapacks --worlds-path ./worlds --shaders-path ./shaders")
        assertEquals("", output.stderr, "Command failed to execute")
        val config = ConfigFile.readOrNull()
        assertNotNull(config, "Config file should be created")
        assertEquals("foo", config.getName())
        assertEquals("1.20.1", config.getVersion())
        assertEquals("bar", config.getDescription())
        assertEquals("test", config.getAuthor())
        assertEquals("./dummy-mods", config.paths[ProjectType.MOD.serialName])
        assertEquals("./dummy-resourcepacks", config.paths[ProjectType.RESOURCE_PACK.serialName])
        assertEquals("./datapacks", config.paths[ProjectType.DATA_PACK.serialName])
        assertEquals("./worlds", config.paths[ProjectType.WORLD.serialName])
        assertEquals("./shaders", config.paths[ProjectType.SHADER.serialName])
    }
}
