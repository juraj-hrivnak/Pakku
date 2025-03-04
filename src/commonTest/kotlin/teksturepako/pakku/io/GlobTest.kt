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

class GlobTest : PakkuTest()
{
    private val testFileName = "test_file.txt"
    private val testDirName = "test_dir"

    override suspend fun `set-up`()
    {
        createTestFile(testFileName)
        createTestDir(testDirName)
        createTestFile(testDirName, testFileName)
    }

    @Test
    fun `test with basic file name`(): Unit = runBlocking {
        val expandedGlob = listOf(testFileName).expandWithGlob(Path(workingPath))

        expectThat(expandedGlob).contains(testFileName)
    }

    @Test
    fun `test with negating pattern`(): Unit = runBlocking {
        val expandedGlob = listOf("!$testFileName").expandWithGlob(Path(workingPath))

        expectThat(expandedGlob).doesNotContain(testFileName)
    }

    @Test
    fun `test with dir pattern`(): Unit = runBlocking {
        val expandedGlob = listOf("$testDirName/**").expandWithGlob(Path(workingPath))

        expectThat(expandedGlob).doesNotContain(testDirName)
        expectThat(expandedGlob).contains(Path(testDirName, testFileName).pathString)
    }
}