package teksturepako.pakku.api.platforms

import io.mockk.every
import io.mockk.mockk
import teksturepako.pakku.api.models.cf.CfModModel
import teksturepako.pakku.api.platforms.CurseForge.LOADER_VERSION_TYPE_ID
import teksturepako.pakku.api.platforms.CurseForge.sortByLoaders
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CurseForgeTest
{
    @Test
    fun requestProject()
    {
    }

    @Test
    fun sortByLoaders_WithValidLoaders_ShouldSortCorrectly() {
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
        val sortedFiles = files.toList().sortByLoaders(loaders)

        assertContentEquals(listOf(files[1], files[3], files[0], files[2]), sortedFiles)
    }

    @Test
    fun sortByLoaders_WithNoMatchingLoaders_ShouldNotChangeOrder() {
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
        val sortedFiles = files.toList().sortByLoaders(loaders)
        assertContentEquals(files, sortedFiles)
    }

    @Test
    fun sortByLoaders_WithSomeMatchingLoaders_ShouldSortCorrectly() {
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
        val sortedFiles = files.toList().sortByLoaders(loaders)
        assertEquals(files[0], sortedFiles[1])
    }
}