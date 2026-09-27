package teksturepako.pakku.io

import kotlinx.coroutines.test.runTest
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.expectStructure
import teksturepako.pakku.testStructure
import teksturepako.pakku.toPrettyString
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.fail

class CopyRecursiveTest : PakkuTest()
{
    private val testFileContent = "Hello, Pakku!"

    // -- SINGLE FILE COPY TESTS --

    @Test
    fun `copy single file`() = runTest {
        val struct = testStructure {
            file("source.txt", testFileContent)
        }

        with(struct) {
            file("source.txt").copyRecursivelyTo(testPath("dest.txt"))
                ?.onError { fail() }
        }

        expectStructure {
            file("source.txt", testFileContent)
            file("dest.txt", testFileContent)
        }
    }

    @Test
    fun `copy single file from absolute path`() = runTest {
        val struct = testStructure {
            file("source.txt", testFileContent)
        }

        with(struct) {
            val absoluteSource = file("source.txt").toAbsolutePath()
            val absoluteDest = testPath("dest.txt").toAbsolutePath()

            absoluteSource.copyRecursivelyTo(absoluteDest)
                ?.onError { fail("Absolute paths should be allowed for filesystem copy: $it") }
        }

        expectStructure {
            file("source.txt", testFileContent)
            file("dest.txt", testFileContent)
        }
    }

    @Test
    fun `invalid file`() = runTest {
        val struct = testStructure {
            file("../source.txt", testFileContent)
        }

        with(struct) {
            file("../source.txt").copyRecursivelyTo(testPath("dest.txt"))
        }

        expectStructure {
            doesNotExist("dest.txt")
        }
    }

    @Test
    fun `file with the same hash`() = runTest {
        val struct = testStructure {
            file("test_file.txt", testFileContent)
        }

        with(struct) {
            // First copy
            file("test_file.txt").copyRecursivelyTo(testPath("copied_test_file.txt"))?.onError { fail() }

            // Second copy - should skip because hash matches
            file("test_file.txt").copyRecursivelyTo(
            testPath("copied_test_file.txt"), onAction = {
                println(it.toPrettyString())
                fail("Should not copy file with same hash")
            })?.onError { fail() }
        }

        expectStructure {
            file("copied_test_file.txt", testFileContent)
        }
    }

    @Test
    fun `copy single file with different hash should overwrite`() = runTest {
        val struct = testStructure {
            file("source.txt", testFileContent)
            file("dest.txt", "Different content")
        }

        with(struct) {
            file("source.txt").copyRecursivelyTo(file("dest.txt"))?.onError { fail() }
        }

        expectStructure {
            file("dest.txt", testFileContent)
        }
    }

    // -- DIRECTORY COPY TESTS --

    @Test
    fun `copy empty directory`() = runTest {
        val struct = testStructure {
            dir("source_empty")
        }

        with(struct) {
            dir("source_empty").copyRecursivelyTo(testPath("dest_empty"))?.onError { fail() }
        }

        expectStructure {
            doesNotExist("dest_empty")
        }
    }

    @Test
    fun `copy directory with single file`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("test_file.txt", testFileContent)
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(testPath("destination"))?.onError { fail() }
        }

        expectStructure {
            dir("destination") {
                file("test_file.txt", testFileContent)
            }
        }
    }

    @Test
    fun `copy directory with multiple files`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("file1.txt", "Content 1")
                file("file2.txt", "Content 2")
                file("file3.txt", "Content 3")
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(testPath("destination"))?.onError { fail() }
        }

        expectStructure {
            dir("destination") {
                file("file1.txt", "Content 1")
                file("file2.txt", "Content 2")
                file("file3.txt", "Content 3")
            }
        }
    }

    @Test
    fun `copy directory with nested subdirectories`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("root_file.txt", "Root")
                dir("subdir1") {
                    file("nested_file.txt", "Nested")
                    dir("subdir2") {
                        file("deeply_nested_file.txt", "Deeply nested")
                    }
                }
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(testPath("destination"))?.onError { fail() }
        }

        expectStructure {
            dir("destination") {
                file("root_file.txt", "Root")
                dir("subdir1") {
                    file("nested_file.txt", "Nested")
                    dir("subdir2") {
                        file("deeply_nested_file.txt", "Deeply nested")
                    }
                }
            }
        }
    }

    // -- CLEANUP TESTS --

    @Test
    fun `cleanup removes files not in source`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("keep_file.txt", "Keep this")
            }
            dir("dest") {
                file("keep_file.txt", "Keep this")
                file("remove_file.txt", "Remove this")
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(dir("dest"), cleanUp = true)?.onError { fail() }
        }

        expectStructure {
            dir("dest") {
                file("keep_file.txt", "Keep this")
                doesNotExist("remove_file.txt")
            }
        }
    }

    @Test
    fun `cleanup disabled preserves extra files`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("source_file.txt", "Source")
            }
            dir("dest") {
                file("extra_file.txt", "Extra")
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(dir("dest"), cleanUp = false)?.onError { fail() }
        }

        expectStructure {
            dir("dest") {
                file("source_file.txt", "Source")
                file("extra_file.txt", "Extra")
            }
        }
    }

    @Test
    fun `cleanup removes empty directories`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("file.txt", "Content")
            }
            dir("dest") {
                file("file.txt", "Content")
                dir("empty_dir") {
                    file("to_remove.txt", "This will be removed")
                }
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(dir("dest"), cleanUp = true)?.onError { fail() }
        }

        expectStructure {
            dir("dest") {
                file("file.txt", "Content")
                doesNotExist("empty_dir")
            }
        }
    }

    // -- HASH OPTIMIZATION TESTS --

    @Test
    fun `identical files are not recopied`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("file1.txt", "Same content")
                file("file2.txt", "Different content")
            }
        }

        with(struct) {
            // First copy
            dir("source").copyRecursivelyTo(testPath("dest"), cleanUp = false)?.onError { fail() }

            // Modify only one file
            file("source", "file2.txt").writeText("Updated content")

            // Second copy - should only copy file2
            dir("source").copyRecursivelyTo(testPath("dest"), cleanUp = false)?.onError { fail() }
        }

        expectStructure {
            dir("dest") {
                file("file1.txt", "Same content")
                file("file2.txt", "Updated content")
            }
        }
    }

    // -- ERROR HANDLING TESTS --

    @Test
    fun `copy invalid path returns error`() = runTest {
        val error = testPath("non_existent_file.txt").copyRecursivelyTo(testPath("destination.txt"))
        assertIs<InvalidPathError>(assertNotNull(error))
    }

    // -- FILE ACTION TESTS --

    @Test
    fun `FileCopied action contains correct information`() = runTest {
        val struct = testStructure {
            file("test_file.txt", testFileContent)
        }

        var capturedAction: FileAction? = null

        with(struct) {
            file("test_file.txt").copyRecursivelyTo(
                testPath("copied_test_file.txt"), onAction = { capturedAction = it })?.onError { fail() }
        }

        val copied = assertIs<FileAction.FileCopied>(assertNotNull(capturedAction))
        assert(copied.source == struct.file("test_file.txt"))
        assert(copied.destination == testPath("copied_test_file.txt"))
        assertNotNull(copied.hash)
        assert("copied file" in copied.description)
    }

    @Test
    fun `FileDeleted action during cleanup`() = runTest {
        val struct = testStructure {
            dir("source") {
                file("keep. txt", "Keep")
            }
            dir("dest") {
                file("delete.txt", "Delete")
            }
        }

        val actions = mutableListOf<FileAction>()

        with(struct) {
            dir("source").copyRecursivelyTo(
                dir("dest"), onAction = { actions.add(it) }, cleanUp = true
            )?.onError { fail() }
        }

        val deletedAction = assertNotNull(actions.filterIsInstance<FileAction.FileDeleted>().firstOrNull())
        assertNotNull(deletedAction.hash)
        assert("deleted file" in deletedAction.description)
    }

    @Test
    fun `DirectoryDeleted action during cleanup`() = runTest {
        val struct = testStructure {
            dir("source")
            dir("dest") {
                dir("empty_subdir")
            }
        }

        val actions = mutableListOf<FileAction>()

        with(struct) {
            dir("source").copyRecursivelyTo(
                dir("dest"), onAction = { actions.add(it) }, cleanUp = true
            )?.onError { fail() }
        }

        val dirDeletedAction = assertNotNull(actions.filterIsInstance<FileAction.DirectoryDeleted>().firstOrNull())
        assert("deleted empty directory" in dirDeletedAction.description)
    }

    // -- EDGE CASES --

    @Test
    fun `copy with special characters in filename`() = runTest {
        val specialFileName = "test file with spaces & special-chars_123.txt"

        val struct = testStructure {
            dir("source") {
                file(specialFileName, "Special content")
            }
        }

        with(struct) {
            dir("source").copyRecursivelyTo(testPath("destination"))?.onError { fail() }
        }

        expectStructure {
            dir("destination") {
                file(specialFileName, "Special content")
            }
        }
    }

    @Test
    fun `copy preserves file content exactly`() = runTest {
        val binaryContent = ByteArray(256) { it.toByte() }

        val struct = testStructure {
            file("binary_file.bin", binaryContent)
        }

        with(struct) {
            file("binary_file.bin").copyRecursivelyTo(testPath("copied_binary_file.bin"))
                ?.onError { fail() }
        }

        expectStructure {
            file("copied_binary_file.bin", binaryContent)
        }
    }
}