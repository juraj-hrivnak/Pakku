package teksturepako.pakku.io

import kotlinx.coroutines.runBlocking
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.toPrettyString
import kotlin.io.path.*
import kotlin.test.fail

class CopyRecursiveTest : PakkuTest()
{
    private val sourceDir = "source"
    private val destDir = "destination"
    private val testFileName = "test_file.txt"
    private val testFileContent = "Hello, Pakku!"

    // -- SINGLE FILE COPY TESTS --

    @Test fun `file to other file`(): Unit = runBlocking {
        createTestFile(testFileName)
        testFile(testFileName).writeText(testFileContent)

        val source = testFile(testFileName)
        val destination = testFile("copied_$testFileName")

        val error = source.copyRecursivelyTo(destination, onAction = { println(it.toPrettyString()) })

        expectThat(error).isNull()
        expectThat(destination).get { exists() }.isTrue()
        expectThat(destination.readText()).isEqualTo(testFileContent)
    }

    @Test fun `file with the same hash`(): Unit = runBlocking {
        createTestFile(testFileName)
        testFile(testFileName).writeText(testFileContent)

        val source = testFile(testFileName)
        val destination = testFile("copied_$testFileName")

        // First copy
        source.copyRecursivelyTo(destination, onAction = { println(it.toPrettyString()) })

        // Second copy - should skip because hash matches
        val error = source.copyRecursivelyTo(destination, onAction = { println(it.toPrettyString()); fail() })

        expectThat(error).isNull()
        expectThat(destination).get { exists() }.isTrue()
        expectThat(destination.readText()).isEqualTo(testFileContent)
    }

    @Test
    fun `test copy single file with different hash should overwrite`(): Unit = runBlocking {
        createTestFile(testFileName)
        testFile(testFileName).writeText(testFileContent)

        val source = testFile(testFileName)
        val destination = testFile("copied_$testFileName")

        // Create destination with different content
        createTestFile("copied_$testFileName")
        destination.writeText("Different content")

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(destination.readText()).isEqualTo(testFileContent)
    }

    // -- DIRECTORY COPY TESTS --

    @Test
    fun `test copy empty directory`(): Unit = runBlocking {
        val sourceDir = "source_empty"
        val destDir = "dest_empty"

        createTestDir(sourceDir)

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(destination). get { exists() }.isFalse()
    }

    @Test
    fun `test copy directory with single file`(): Unit = runBlocking {
        createTestDir(sourceDir)
        createTestFile(sourceDir, testFileName)
        testFile(sourceDir, testFileName).writeText(testFileContent)

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(testFile(destDir, testFileName)).get { exists() }.isTrue()
        expectThat(testFile(destDir, testFileName).readText()).isEqualTo(testFileContent)
    }

    @Test
    fun `test copy directory with multiple files`(): Unit = runBlocking {
        createTestDir(sourceDir)
        createTestFile(sourceDir, "file1.txt")
        createTestFile(sourceDir, "file2.txt")
        createTestFile(sourceDir, "file3.txt")

        testFile(sourceDir, "file1.txt").writeText("Content 1")
        testFile(sourceDir, "file2.txt").writeText("Content 2")
        testFile(sourceDir, "file3.txt").writeText("Content 3")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "file1.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "file2.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "file3.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "file1.txt").readText()).isEqualTo("Content 1")
        expectThat(testFile(destDir, "file2.txt").readText()).isEqualTo("Content 2")
        expectThat(testFile(destDir, "file3.txt").readText()).isEqualTo("Content 3")
    }

    @Test
    fun `test copy directory with nested subdirectories`(): Unit = runBlocking {
        createTestDir(sourceDir)
        createTestDir(sourceDir, "subdir1")
        createTestDir(sourceDir, "subdir1", "subdir2")

        createTestFile(sourceDir, "root_file.txt")
        createTestFile(sourceDir, "subdir1", "nested_file.txt")
        createTestFile(sourceDir, "subdir1", "subdir2", "deeply_nested_file.txt")

        testFile(sourceDir, "root_file.txt").writeText("Root")
        testFile(sourceDir, "subdir1", "nested_file.txt").writeText("Nested")
        testFile(sourceDir, "subdir1", "subdir2", "deeply_nested_file.txt").writeText("Deeply nested")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "root_file.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "subdir1", "nested_file.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "subdir1", "subdir2", "deeply_nested_file.txt")).get { exists() }.isTrue()
        expectThat(
            testFile(
                destDir,
                "subdir1",
                "subdir2",
                "deeply_nested_file.txt"
            ).readText()
        ).isEqualTo("Deeply nested")
    }

    // -- CLEANUP TESTS --

    @Test
    fun `test cleanup removes files not in source`(): Unit = runBlocking {
        val sourceDir = "source_cleanup"
        val destDir = "dest_cleanup"

        createTestDir(sourceDir)
        createTestFile(sourceDir, "keep_file.txt")
        testFile(sourceDir, "keep_file.txt").writeText("Keep this")

        createTestDir(destDir)
        createTestFile(destDir, "keep_file.txt")
        createTestFile(destDir, "remove_file.txt")
        testFile(destDir, "keep_file.txt").writeText("Keep this")  // Same content = same hash
        testFile(destDir, "remove_file.txt").writeText("Remove this")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination, cleanUp = true)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "keep_file.txt")). get { exists() }.isTrue()
        expectThat(testFile(destDir, "remove_file.txt")).get { exists() }. isFalse()
    }

    @Test
    fun `test cleanup disabled preserves extra files`(): Unit = runBlocking {
        createTestDir(sourceDir)
        createTestFile(sourceDir, "source_file.txt")
        testFile(sourceDir, "source_file.txt").writeText("Source")

        createTestDir(destDir)
        createTestFile(destDir, "extra_file.txt")
        testFile(destDir, "extra_file.txt").writeText("Extra")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination, cleanUp = false)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "source_file.txt")).get { exists() }.isTrue()
        expectThat(testFile(destDir, "extra_file.txt")).get { exists() }.isTrue()
    }
    @Test
    fun `test cleanup removes empty directories`(): Unit = runBlocking {
        val sourceDir = "source_cleanup_dirs"
        val destDir = "dest_cleanup_dirs"

        createTestDir(sourceDir)
        createTestFile(sourceDir, "file. txt")
        testFile(sourceDir, "file.txt"). writeText("Content")

        createTestDir(destDir)
        createTestDir(destDir, "empty_dir")
        createTestFile(destDir, "empty_dir", "to_remove. txt")
        testFile(destDir, "empty_dir", "to_remove.txt").writeText("This will be removed")
        createTestFile(destDir, "file.txt")
        testFile(destDir, "file. txt").writeText("Content")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination, cleanUp = true)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "empty_dir")). get { exists() }. isFalse()
    }

    // -- HASH OPTIMIZATION TESTS --

    @Test
    fun `test identical files are not recopied`(): Unit = runBlocking {
        val sourceDir = "source_hash_opt"
        val destDir = "dest_hash_opt"

        createTestDir(sourceDir)
        createTestFile(sourceDir, "file1. txt")
        createTestFile(sourceDir, "file2.txt")
        testFile(sourceDir, "file1.txt"). writeText("Same content")
        testFile(sourceDir, "file2.txt"). writeText("Different content")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        // First copy
        source.copyRecursivelyTo(destination, cleanUp = false)

        // Modify only one file
        testFile(sourceDir, "file2.txt").writeText("Updated content")

        // Second copy - should only copy file2
        val error = source.copyRecursivelyTo(destination, cleanUp = false)

        expectThat(error).isNull()
        expectThat(testFile(destDir, "file1.txt").readText()).isEqualTo("Same content")
        expectThat(testFile(destDir, "file2.txt").readText()). isEqualTo("Updated content")
    }

    // -- ERROR HANDLING TESTS --

    @Test
    fun `test copy invalid path returns error`(): Unit = runBlocking {
        val invalidPath = testFile("non_existent_file.txt")
        val destination = testFile("destination.txt")

        val error = invalidPath.copyRecursivelyTo(destination)

        expectThat(error).isNotNull().isA<InvalidPathError>()
    }

    // -- FILE ACTION TESTS --

    @Test
    fun `test FileCopied action contains correct information`(): Unit = runBlocking {
        createTestFile(testFileName)
        testFile(testFileName).writeText(testFileContent)

        val source = testFile(testFileName)
        val destination = testFile("copied_$testFileName")

        var capturedAction: FileAction? = null
        source.copyRecursivelyTo(destination, onAction = { capturedAction = it })

        expectThat(capturedAction).isNotNull().isA<FileAction.FileCopied>().and {
            get { this.source }.isEqualTo(source)
            get { this.destination }.isEqualTo(destination)
            get { this.hash }.isNotNull()
            get { this.description }.contains("copied file")
        }
    }

    @Test
    fun `test FileDeleted action during cleanup`(): Unit = runBlocking {
        createTestDir(sourceDir)
        createTestFile(sourceDir, "keep. txt")
        testFile(sourceDir, "keep.txt").writeText("Keep")

        createTestDir(destDir)
        createTestFile(destDir, "delete.txt")
        testFile(destDir, "delete.txt").writeText("Delete")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val actions = mutableListOf<FileAction>()
        source.copyRecursivelyTo(destination, onAction = { actions.add(it) }, cleanUp = true)

        val deletedAction = actions.filterIsInstance<FileAction.FileDeleted>().firstOrNull()

        expectThat(deletedAction).isNotNull().and {
            get { this.hash }.isNotNull()
            get { this.description }.contains("deleted file")
        }
    }

    @Test
    fun `test DirectoryDeleted action during cleanup`(): Unit = runBlocking {
        createTestDir(sourceDir)

        createTestDir(destDir)
        createTestDir(destDir, "empty_subdir")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val actions = mutableListOf<FileAction>()
        source.copyRecursivelyTo(destination, onAction = { actions.add(it) }, cleanUp = true)

        val dirDeletedAction = actions.filterIsInstance<FileAction.DirectoryDeleted>().firstOrNull()

        expectThat(dirDeletedAction).isNotNull().and {
            get { this.description }.contains("deleted empty directory")
        }
    }

    // -- EDGE CASES --

    @Test
    fun `test copy with special characters in filename`(): Unit = runBlocking {
        val specialFileName = "test file with spaces & special-chars_123.txt"
        createTestDir(sourceDir)
        createTestFile(sourceDir, specialFileName)
        testFile(sourceDir, specialFileName).writeText("Special content")

        val source = testFile(sourceDir)
        val destination = testFile(destDir)

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(testFile(destDir, specialFileName)).get { exists() }.isTrue()
        expectThat(testFile(destDir, specialFileName).readText()).isEqualTo("Special content")
    }

    @Test
    fun `test copy preserves file content exactly`(): Unit = runBlocking {
        createTestFile(testFileName)
        val binaryContent = ByteArray(256) { it.toByte() }
        testFile(testFileName).writeBytes(binaryContent)

        val source = testFile(testFileName)
        val destination = testFile("copied_$testFileName")

        val error = source.copyRecursivelyTo(destination)

        expectThat(error).isNull()
        expectThat(destination.readBytes()).contentEquals(binaryContent)
    }
}