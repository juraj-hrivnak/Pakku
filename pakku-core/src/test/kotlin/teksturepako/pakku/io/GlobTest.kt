package teksturepako.pakku.io

import kotlinx.coroutines.test.runTest
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test

class GlobTest : PakkuTest(teardown = true)
{
    @Test
    fun `test glob of single file`() = runTest {
        val file = "test_file.txt"
        createTestFile(file)

        val expandedGlob = listOf(file).expandWithGlob(Path(workingPath))

        assert(file in expandedGlob)
    }

    @Test
    fun `test negating glob of single file`() = runTest {
        val file = "test_file.txt"
        createTestFile(file)

        val expandedGlob = listOf("!$file").expandWithGlob(Path(workingPath))

        assert(file !in expandedGlob)
    }

    @Test
    fun `test all dir content glob with one file negated`() = runTest {
        val dir = "test_dir"
        createTestDir(dir)

        val includedFile = "included_file.txt"
        createTestFile(dir, includedFile)

        val excludedFile = "excluded_file.txt"
        createTestFile(dir, excludedFile)

        val expandedGlob = listOf(
            "$dir/**",
            "!$dir/$excludedFile"
        ).expandWithGlob(Path(workingPath))

        assert(Path(dir, includedFile).pathString in expandedGlob)
        assert(Path(dir, excludedFile).pathString !in expandedGlob)
    }

    @Test
    fun `test nested sub dirs with content`() = runTest {
        val dir = "test_dir"
        createTestDir(dir)

        val subDir = "sub_dir"
        createTestDir(dir, subDir)

        val includedFile = "included_file.txt"
        createTestFile(dir, includedFile)

        val excludedFile = "excluded_file.txt"
        createTestFile(dir, excludedFile)

        val excludedFileInSubDir = "excluded_file_2.txt"
        createTestFile(dir, subDir, excludedFile)

        val expandedGlob = listOf(
            "$dir/**",
            "!$dir/$excludedFile",
            "!$dir/$subDir",
            "!$dir/$subDir/**"
        ).expandWithGlob(Path(workingPath))

        assert(Path(dir, subDir).pathString !in expandedGlob)
        assert(Path(dir, includedFile).pathString in expandedGlob)
        assert(Path(dir, excludedFile).pathString !in expandedGlob)
        assert(Path(dir, subDir, excludedFileInSubDir).pathString !in expandedGlob)
    }

    @Test
    fun `test simple sub dir negating`() = runTest {
        val dir = "test_dir"
        createTestDir(dir)

        val subDir = "sub_dir"
        createTestDir(dir, subDir)

        val includedFile = "included.txt"
        createTestFile(dir, includedFile)
        createTestFile(dir, subDir, "excluded.txt")

        val expandedGlob = listOf(
            dir,
            "!$dir/$subDir",
        ).expandWithGlob(Path(workingPath))

        assert(Path(dir, includedFile).pathString in expandedGlob)
        assert(Path(dir, subDir, "excluded.txt").pathString !in expandedGlob)
    }

    @Test
    fun `test sub dir negating with content`() = runTest {
        val dir = "test_dir"
        createTestDir(dir)

        val subDir = "sub_dir"
        createTestDir(dir, subDir)

        val file = "test_file.txt"
        createTestFile(dir, file)
        createTestFile(dir, subDir, file)

        val file2 = "test_file_2.txt"
        createTestFile(dir, subDir, file2)

        val expandedGlob = listOf(
            dir,
            "!$dir/$subDir/",
            "$dir/$subDir/$file",
        ).expandWithGlob(Path(workingPath))

        assert(Path(dir, file).pathString in expandedGlob)
        assert(Path(dir, subDir, file).pathString in expandedGlob)
        assert(Path(dir, subDir, file2).pathString !in expandedGlob)
    }

    @Test
    fun `test triple subdirectories negating pattern`() = runTest {

        // -- USING: '**' --

        val firstDir = "dir_1"
        createTestDir(firstDir)

        val secondDir = "dir_2"
        createTestDir(firstDir, secondDir)

        val thirdDir = "dir_3"
        createTestDir(firstDir, secondDir, thirdDir)

        val file = "test_file.txt"
        createTestFile(firstDir, secondDir, file)

        val file2 = "test_file_2.txt"
        createTestFile(firstDir, secondDir, thirdDir, file2)

        val expandedGlob = listOf(
            "$firstDir/**",
            "!$firstDir/$secondDir/$file",
        ).expandWithGlob(Path(workingPath))

        assert(Path(firstDir, secondDir, thirdDir, file2).pathString in expandedGlob)
        assert(Path(firstDir, secondDir, file).pathString !in expandedGlob)

        // -- USING: '*' --
        // Matches the subdirectory, which expands to its files (not the directory path itself).

        val expandedGlobsSingleWildcard = listOf(
            "$firstDir/*",
            "!$firstDir/$secondDir/$file",
        ).expandWithGlob(Path(workingPath))

        assert(expandedGlobsSingleWildcard == listOf(Path(firstDir, secondDir, thirdDir, file2).pathString))

        // -- USING NO WILDCARDS --
        // A bare directory pattern expands to all files under that directory.

        val expandedGlobWithoutWildcards = listOf(
            firstDir,
            "!$firstDir/$secondDir/$file",
        ).expandWithGlob(Path(workingPath))

        assert(expandedGlobWithoutWildcards == listOf(Path(firstDir, secondDir, thirdDir, file2).pathString))
    }
}
