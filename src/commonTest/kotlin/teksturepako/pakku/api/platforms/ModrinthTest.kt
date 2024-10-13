package teksturepako.pakku.api.platforms

import io.mockk.every
import io.mockk.mockk
import teksturepako.pakku.api.models.mr.MrVersionModel
import teksturepako.pakku.api.platforms.Modrinth.sortByLoaders
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ModrinthTest
{
    @Test
    fun sortByLoaders_WithValidLoaders_ShouldSortCorrectly() {
        val versions = listOf(
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderb", "loaderc")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loadera", "loaderb")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderb", "loaderb")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loadera", "loaderc")
            }
        )

        val loaders = listOf("loadera", "loaderb", "loaderc")
        val sortedVersions = versions.toList().sortByLoaders(loaders)

        assertContentEquals(listOf(versions[1], versions[3], versions[0], versions[2]), sortedVersions)
    }

    @Test
    fun sortByLoaders_WithNoMatchingLoaders_ShouldNotChangeOrder() {
        val versions = listOf(
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderB")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderA")
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedVersions = versions.toList().sortByLoaders(loaders)

        assertContentEquals(versions, sortedVersions)
    }

    @Test
    fun sortByLoaders_WithSomeMatchingLoaders_ShouldSortCorrectly() {
        val versions = listOf(
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loadera")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loader1")
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedVersions = versions.toList().sortByLoaders(loaders)

        assertEquals(versions[0], sortedVersions[1])
    }
}