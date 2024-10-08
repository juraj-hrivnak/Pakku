package teksturepako.pakku.api.projects

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectTest
{
    @Test
    fun hasAliasOf_whenProjectHasAlias_returnsTrue()
    {
        val project1 = mockk<Project>() {
            every { id } returns mutableMapOf("id1" to "id1")
            every { name } returns mutableMapOf("name1" to "name1")
            every { slug } returns mutableMapOf("slug1" to "slug1")
            every { aliases } returns mutableSetOf("id3")
            every { hasAliasOf(any()) } answers { callOriginal() }
        }
        val project2 = mockk<Project>() {
            every { id } returns mutableMapOf("id2" to "id2")
            every { name } returns mutableMapOf("name2" to "name2")
            every { slug } returns mutableMapOf("slug2" to "slug2")
            every { aliases } returns mutableSetOf("name1")
            every { hasAliasOf(any()) } answers { callOriginal() }
        }
        val project3 = mockk<Project>() {
            every { id } returns mutableMapOf("id3" to "id3")
            every { name } returns mutableMapOf("name3" to "name3")
            every { slug } returns mutableMapOf("slug3" to "slug3")
            every { aliases } returns mutableSetOf("slug2")
            every { hasAliasOf(any()) } answers { callOriginal() }
        }

        assertTrue(project1 hasAliasOf project3)
        assertTrue(project2 hasAliasOf project1)
        assertTrue(project3 hasAliasOf project2)
    }
}