package teksturepako.pakku.api.actions.update

import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateTypeTest : PakkuTest()
{
    private fun project(type: ProjectType, slug: String, vararg files: ProjectFile) = Project(
        type = type,
        id = mutableMapOf(Modrinth.serialName to "id-$slug"),
        name = mutableMapOf(Modrinth.serialName to "name-$slug"),
        slug = mutableMapOf(Modrinth.serialName to slug),
        files = files.toMutableSet(),
    )

    private fun file(slug: String) = ProjectFile(
        type = Modrinth.serialName,
        fileName = "$slug.jar",
        parentId = "id-$slug",
        loaders = mutableListOf("datapack"),
    )

    @Test
    fun `combineProjects adopts the freshly fetched type when the locked type is stale`()
    {
        // A datapack locked as a mod: this is the state left behind by the old
        // project_type-only detection.
        val lockedProject = project(ProjectType.MOD, "jjthunder", file("jjthunder"))
        val fetchedProject = project(ProjectType.DATA_PACK, "jjthunder", file("jjthunder"))

        val merged = combineProjects(lockedProject, fetchedProject, Modrinth.serialName, 5)

        assertEquals(ProjectType.DATA_PACK, merged.type)
    }

    @Test
    fun `combineProjects keeps the locked type when types match`()
    {
        val lockedProject = project(ProjectType.MOD, "sodium", file("sodium"))
        val fetchedProject = project(ProjectType.MOD, "sodium", file("sodium"))

        val merged = combineProjects(lockedProject, fetchedProject, Modrinth.serialName, 5)

        assertEquals(ProjectType.MOD, merged.type)
    }

    @Test
    fun `combineProjects keeps the accumulator pakku id when adopting a new type`()
    {
        val lockedProject = project(ProjectType.MOD, "jjthunder", file("jjthunder"))
            .copy(pakkuId = "pakku-id-1234")
        val fetchedProject = project(ProjectType.DATA_PACK, "jjthunder", file("jjthunder"))

        val merged = combineProjects(lockedProject, fetchedProject, Modrinth.serialName, 5)

        assertEquals(ProjectType.DATA_PACK, merged.type)
        assertEquals("pakku-id-1234", merged.pakkuId)
    }
}
