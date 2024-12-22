package teksturepako.pakku.io

import kotlinx.coroutines.runBlocking
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertContains

class GlobTest : PakkuTest()
{
    private val testFileName = "test_file.txt"
    private val testDirName = "test_dir"

    override suspend fun `on-set-up`()
    {
        createTestFile(testFileName)
        createTestDir(testDirName)
        createTestFile(testDirName, testFileName)
    }

    @Test
    fun `test with basic file name`() = runBlocking {
        val expandedGlob = listOf(testFileName).expandWithGlob(Path(workingPath))

        assertContains(expandedGlob, testFileName)
    }

    @Test
    fun `test with negating pattern`() = runBlocking {
        val expandedGlob = listOf("!$testFileName").expandWithGlob(Path(workingPath))

        assert(testFileName !in expandedGlob)
    }

    @Test
    fun `test with dir pattern`() = runBlocking {
        val expandedGlob = listOf("$testDirName/**").expandWithGlob(Path(workingPath))

        assert(testDirName !in expandedGlob) { "$expandedGlob should not contain $testDirName" }

        assertContains(expandedGlob, Path(testDirName, testFileName).pathString)
    }
}