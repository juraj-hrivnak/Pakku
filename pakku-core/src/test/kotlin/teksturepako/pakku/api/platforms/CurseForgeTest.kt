package teksturepako.pakku.api.platforms

import io.mockk.every
import io.mockk.mockk
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.models.cf.CfModModel
import teksturepako.pakku.api.platforms.CurseForge.LOADER_VERSION_TYPE_ID
import teksturepako.pakku.api.platforms.CurseForge.compareByLoaders
import kotlin.test.Test

class CurseForgeTest : PakkuTest()
{
    @Test
    fun sortByLoaders_WithValidLoaders_ShouldCompareCorrectly() {
        val files = listOf(
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderB"
                    },
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderC"
                    }
                )
            },
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderA"
                    },
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderB"
                    }
                )
            },
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderB"
                    },
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderB"
                    }
                )
            },
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderA"
                    },
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderC"
                    }
                )
            }
        )

        val loaders = listOf("loadera", "loaderb", "loaderc")
        val sortedFiles = files.toList().sortedWith(compareBy(compareByLoaders(loaders)))

        assert(sortedFiles == listOf(files[1], files[3], files[0], files[2]))
    }

    @Test
    fun compareByLoaders_WithNoMatchingLoaders_ShouldNotChangeOrder() {
        val files = listOf(
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderB"
                    }
                )
            },
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderA"
                    }
                )
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedFiles = files.toList().sortedWith(compareBy(compareByLoaders(loaders)))
        assert(sortedFiles == files)
    }

    @Test
    fun sortByLoaders_WithSomeMatchingLoaders_ShouldCompareCorrectly() {
        val files = listOf(
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loaderA"
                    }
                )
            },
            mockk<CfModModel.File> {
                every { sortableGameVersions } returns listOf(
                    mockk<CfModModel.File.SortableGameVersion> {
                        every { gameVersionTypeId } returns LOADER_VERSION_TYPE_ID
                        every { gameVersionName } returns "loader1"
                    }
                )
            }
        )

        val loaders = listOf("loader1", "loader2")
        val sortedFiles = files.toList().sortedWith(compareBy(compareByLoaders(loaders)))
        assert(sortedFiles[1] == files[0])
    }
}