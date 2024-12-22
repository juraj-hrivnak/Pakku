package teksturepako.pakku.api.projects

import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectTest
{
    @Test
    fun `test aliases`()
    {
        val project1 = Project(
            type = ProjectType.MOD,
            id = mutableMapOf("id1" to "id1"),
            name = mutableMapOf("name1" to "name1"),
            slug = mutableMapOf("slug1" to "slug1"),
            aliases = mutableSetOf("id3"),
            files = mutableSetOf()
        )
        val project2 = Project(
            type = ProjectType.MOD,
            id = mutableMapOf("id2" to "id2"),
            name = mutableMapOf("name2" to "name2"),
            slug = mutableMapOf("slug2" to "slug2"),
            aliases = mutableSetOf("name1"),
            files = mutableSetOf()
        )
        val project3 = Project(
            type = ProjectType.MOD,
            id = mutableMapOf("id3" to "id3"),
            name = mutableMapOf("name3" to "name3"),
            slug = mutableMapOf("slug3" to "slug3"),
            aliases = mutableSetOf("slug2"),
            files = mutableSetOf()
        )

        assertTrue(project1 hasAliasOf project3)
        assertTrue(project2 hasAliasOf project1)
        assertTrue(project3 hasAliasOf project2)
    }
}