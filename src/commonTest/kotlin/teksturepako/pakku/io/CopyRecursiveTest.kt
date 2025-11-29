package teksturepako.pakku.io

import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.expectStructure
import teksturepako.pakku.testStructure
import teksturepako.pakku.toPrettyString
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.fail

class CopyRecursiveTest : PakkuTest()
{
    private val testFileContent = "Hello, Pakku!"

    // -- SINGLE FILE COPY TESTS --

    @Test
    fun `copy single file`(): Unit = runBlocking {
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
    fun `file with the same hash`(): Unit = runBlocking {
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
    fun `copy single file with different hash should overwrite`(): Unit = runBlocking {
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
    fun `copy empty directory`(): Unit = runBlocking {
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
    fun `copy directory with single file`(): Unit = runBlocking {
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
    fun `copy directory with multiple files`(): Unit = runBlocking {
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
    fun `copy directory with nested subdirectories`(): Unit = runBlocking {
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
    fun `cleanup removes files not in source`(): Unit = runBlocking {
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
    fun `cleanup disabled preserves extra files`(): Unit = runBlocking {
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
    fun `cleanup removes empty directories`(): Unit = runBlocking {
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
    fun `identical files are not recopied`(): Unit = runBlocking {
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
    fun `copy invalid path returns error`(): Unit = runBlocking {
        val error = testPath("non_existent_file.txt").copyRecursivelyTo(testPath("destination.txt"))
        expectThat(error).isNotNull().isA<InvalidPathError>()
    }

    // -- FILE ACTION TESTS --

    @Test
    fun `FileCopied action contains correct information`(): Unit = runBlocking {
        val struct = testStructure {
            file("test_file.txt", testFileContent)
        }

        var capturedAction: FileAction? = null

        with(struct) {
            file("test_file.txt").copyRecursivelyTo(
                testPath("copied_test_file.txt"), onAction = { capturedAction = it })?.onError { fail() }
        }

        expectThat(capturedAction).isNotNull().isA<FileAction.FileCopied>().and {
            get { this.source }.isEqualTo(struct.file("test_file.txt"))
            get { this.destination }.isEqualTo(testPath("copied_test_file.txt"))
            get { this.hash }.isNotNull()
            get { this.description }.contains("copied file")
        }
    }

    @Test
    fun `FileDeleted action during cleanup`(): Unit = runBlocking {
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

        val deletedAction = actions.filterIsInstance<FileAction.FileDeleted>().firstOrNull()

        expectThat(deletedAction).isNotNull().and {
            get { this.hash }.isNotNull()
            get { this.description }.contains("deleted file")
        }
    }

    @Test
    fun `DirectoryDeleted action during cleanup`(): Unit = runBlocking {
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

        val dirDeletedAction = actions.filterIsInstance<FileAction.DirectoryDeleted>().firstOrNull()

        expectThat(dirDeletedAction).isNotNull().and {
            get { this.description }.contains("deleted empty directory")
        }
    }

    // -- EDGE CASES --

    @Test
    fun `copy with special characters in filename`(): Unit = runBlocking {
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
    fun `copy preserves file content exactly`(): Unit = runBlocking {
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