package teksturepako.pakku.api.platforms

import teksturepako.pakku.api.projects.ProjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModrinthTypeTest
{
    @Test fun `mod loader maps to MOD`()
    {
        assertEquals(ProjectType.MOD, Modrinth.getProjectType("mod", listOf("neoforge")))
    }

    @Test fun `resourcepack loader maps to RESOURCE_PACK`()
    {
        assertEquals(ProjectType.RESOURCE_PACK, Modrinth.getProjectType("resourcepack", listOf("minecraft")))
    }

    @Test fun `shader project maps to SHADER`()
    {
        assertEquals(ProjectType.SHADER, Modrinth.getProjectType("shader", listOf("iris")))
    }

    @Test fun `datapack project_type maps to DATA_PACK`()
    {
        assertEquals(ProjectType.DATA_PACK, Modrinth.getProjectType("datapack", listOf("datapack")))
    }

    @Test fun `datapack loader wins over a mod project_type`()
    {
        // Modrinth reports "mod" for projects published under the mod project type
        // even when the project is a datapack. JJThunder To The Max / Hell Is Fire
        // are the reference cases: project_type="mod", loaders=["datapack"].
        assertEquals(ProjectType.DATA_PACK, Modrinth.getProjectType("mod", listOf("datapack")))
    }

    @Test fun `datapack loader is matched case-insensitively`()
    {
        assertEquals(ProjectType.DATA_PACK, Modrinth.getProjectType("mod", listOf("DataPack")))
    }

    @Test fun `minecraft loader alongside datapack still maps to DATA_PACK`()
    {
        // Modrinth lists "minecraft" for datapacks that target the base game, e.g.
        // Cobblemon datapacks such as cobblemotion: project_type="mod", loaders=["datapack","minecraft"].
        assertEquals(ProjectType.DATA_PACK, Modrinth.getProjectType("mod", listOf("datapack", "minecraft")))
    }

    @Test fun `project that also publishes mod-loader versions keeps MOD`()
    {
        // Terralith ships a datapack and mod-loader versions from one project, so it must
        // stay MOD and fetch the mod file rather than the datapack.
        assertEquals(
            ProjectType.MOD,
            Modrinth.getProjectType("mod", listOf("datapack", "fabric", "forge", "neoforge", "quilt"))
        )
    }

    @Test fun `empty loaders fall back to project_type`()
    {
        assertEquals(ProjectType.MOD, Modrinth.getProjectType("mod", listOf()))
    }

    @Test fun `unsupported project_type returns null`()
    {
        assertNull(Modrinth.getProjectType("modpack", listOf("neoforge")))
    }
}
