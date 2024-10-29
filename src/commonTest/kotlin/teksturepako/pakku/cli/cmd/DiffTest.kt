package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.runCatching
import teksturepako.pakku.api.data.workingPath
import java.io.File
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalPathApi::class)
class DiffTest
{
    init
    {
        workingPath = "./build/test/diff"
        runCatching { Path("./build/test/diff").deleteRecursively() }
        runCatching { Path("./build/test/diff").createDirectory() }
        runCatching { Path("build/tmp/jvmTest/diffTest").deleteRecursively() }
        runCatching { Path("build/tmp/jvmTest/diffTest").createDirectory() }
    }

    @Test
    fun `should success diff matches`()
    {
        val cmd = Diff()
        val numberOfTestCases = 1

        val testInputFiles = "src/commonTest/resources/diffTest"
        val testOutputPath = "build/tmp/jvmTest/diffTest"

        for (testNumber in 1..numberOfTestCases)
        {
            val oldLockFile = "$testInputFiles/old/$testNumber.json"
            val newLockFile = "$testInputFiles/new/$testNumber.json"

            val outputFileMD = "$testOutputPath/$testNumber-markdown.md"
            val expectedOutputFileMD = "$testInputFiles/expected_output/$testNumber-markdown.md"

            val outputFileMDVerbose = "$testOutputPath/$testNumber-markdown-verbose.md"
            val expectedOutputFileMDVerbose = "$testInputFiles/expected_output/$testNumber-markdown-verbose.md"

            val outputFileMDDiff = "$testOutputPath/$testNumber-markdown-diff.md"
            val expectedOutputFileMDDiff = "$testInputFiles/expected_output/$testNumber-markdown-diff.md"

            val outputFileMDDiffVerbose = "$testOutputPath/$testNumber-markdown-diff-verbose.md"
            val expectedOutputFileMDDiffVerbose = "$testInputFiles/expected_output/$testNumber-markdown-diff-verbose.md"

            cmd.test("$oldLockFile $newLockFile --markdown $outputFileMD")
            cmd.test("$oldLockFile $newLockFile --verbose --markdown $outputFileMDVerbose")
            cmd.test("$oldLockFile $newLockFile --markdown-diff $outputFileMDDiff")
            cmd.test("$oldLockFile $newLockFile --verbose --markdown-diff $outputFileMDDiffVerbose")

            assertEquals(
                expected = Files.readAllLines(File(expectedOutputFileMD).toPath()),
                actual = Files.readAllLines(File(outputFileMD).toPath()),
                message = "Test diff failed at $testNumber-markdown.md\n"
            )
            assertEquals(
                expected = Files.readAllLines(File(expectedOutputFileMDVerbose).toPath()),
                actual = Files.readAllLines(File(outputFileMDVerbose).toPath()),
                message = "Test diff failed at $testNumber-markdown-verbose.md\n"
            )
            assertEquals(
                expected = Files.readAllLines(File(expectedOutputFileMDDiff).toPath()),
                actual = Files.readAllLines(File(outputFileMDDiff).toPath()),
                message = "Test diff failed at $testNumber-markdown-diff.md\n"
            )
            assertEquals(
                expected = Files.readAllLines(File(expectedOutputFileMDDiffVerbose).toPath()),
                actual = Files.readAllLines(File(outputFileMDDiffVerbose).toPath()),
                message = "Test diff failed at $testNumber-markdown-diff-verbose.md\n"
            )
        }
    }
}
