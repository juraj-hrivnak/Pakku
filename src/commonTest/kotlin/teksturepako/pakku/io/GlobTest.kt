package teksturepako.pakku.io

import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.doesNotContain
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test

class GlobTest : PakkuTest(teardown = true)
{
    @Test
    fun `test glob of single file`(): Unit = runBlocking {
        val file = "test_file.txt"
        createTestFile(file)

        val expandedGlob = listOf(file).expandWithGlob(Path(workingPath))

        expectThat(expandedGlob).contains(file)
    }

    @Test
    fun `test negating glob of single file`(): Unit = runBlocking {
        val file = "test_file.txt"
        createTestFile(file)

        val expandedGlob = listOf("!$file").expandWithGlob(Path(workingPath))

        expectThat(expandedGlob).doesNotContain(file)
    }

    @Test
    fun `test all dir content glob with one file negated`(): Unit = runBlocking {
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

        expectThat(expandedGlob)
            .contains(Path(dir, includedFile).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, excludedFile).pathString)
    }

    @Test
    fun `test nested sub dirs with content`(): Unit = runBlocking {
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

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, subDir).pathString)

        expectThat(expandedGlob)
            .contains(Path(dir, includedFile).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, excludedFile).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, subDir, excludedFileInSubDir).pathString)
    }

    @Test
    fun `test simple sub dir negating`(): Unit = runBlocking {
        val dir = "test_dir"
        createTestDir(dir)

        val subDir = "sub_dir"
        createTestDir(dir, subDir)

        val expandedGlob = listOf(
            dir,
            "!$dir/$subDir",
        ).expandWithGlob(Path(workingPath))

        expectThat(expandedGlob)
            .contains(Path(dir).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, subDir).pathString)

    }

    @Test
    fun `test sub dir negating with content`(): Unit = runBlocking {
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

        expectThat(expandedGlob)
            .contains(Path(dir).pathString)

        expectThat(expandedGlob)
            .contains(Path(dir, subDir, file).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(dir, subDir, file2).pathString)
    }

    @Test
    fun `test triple subdirectories negating pattern`(): Unit = runBlocking {

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

        expectThat(expandedGlob)
            .contains(Path(firstDir, secondDir, thirdDir, file2).pathString)

        expectThat(expandedGlob)
            .doesNotContain(Path(firstDir, secondDir, file).pathString)

        // -- USING: '*' --

        val expandedGlobsSingleWildcard = listOf(
            "$firstDir/*",
            "!$firstDir/$secondDir/$file",
        ).expandWithGlob(Path(workingPath))

        expectThat(expandedGlobsSingleWildcard)
            .containsExactly(Path(firstDir, secondDir).pathString)

        // -- USING NO WILDCARDS --

        val expandedGlobWithoutWildcards = listOf(
            firstDir,
            "!$firstDir/$secondDir/$file",
        ).expandWithGlob(Path(workingPath))

        expectThat(expandedGlobWithoutWildcards)
            .containsExactly(Path(firstDir).pathString)
    }
}