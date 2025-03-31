package teksturepako.pakku.io

import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.contains
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
    fun `test negating nested dirs`(): Unit = runBlocking {
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
}