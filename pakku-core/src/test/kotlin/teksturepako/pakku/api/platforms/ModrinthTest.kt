package teksturepako.pakku.api.platforms

import io.mockk.every
import io.mockk.mockk
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.models.mr.MrVersionModel
import teksturepako.pakku.api.platforms.Modrinth.compareByLoaders
import kotlin.test.Test

class ModrinthTest : PakkuTest()
{
    @Test
    fun compareByLoaders_WithValidLoaders_ShouldSortCorrectly() {
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
        val sortedVersions = versions.toList().sortedWith(compareBy(compareByLoaders(loaders)))

        assert(sortedVersions == listOf(versions[1], versions[3], versions[0], versions[2]))
    }

    @Test
    fun compareByLoaders_ShouldNotChangeOrder() {
        val versions = listOf(
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderB")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loaderA")
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedVersions = versions.toList().sortedWith(compareBy(compareByLoaders(loaders)))

        assert(sortedVersions == versions)
    }

    @Test
    fun compareByLoaders_WithSomeMatchingLoaders_ShouldSortCorrectly() {
        val versions = listOf(
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loadera")
            },
            mockk<MrVersionModel> {
                every { loaders } returns listOf("loader1")
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedVersions = versions.toList().sortedWith(compareBy(compareByLoaders(loaders)))

        assert(sortedVersions[1] == versions[0])
    }
}