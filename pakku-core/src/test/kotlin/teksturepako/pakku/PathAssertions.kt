package teksturepako.pakku

import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun assertPathExists(path: Path) = assertTrue(path.exists(), "expected path to exist: $path")

fun assertPathDoesNotExist(path: Path) = assertFalse(path.exists(), "expected path to not exist: $path")

fun assertIsDirectory(path: Path) = assertTrue(path.isDirectory(), "expected path to be a directory: $path")

fun assertIsFile(path: Path) = assertTrue(path.isRegularFile(), "expected path to be a file: $path")

fun assertHasContent(path: Path, expected: String) = assert(path.readText() == expected)

fun assertHasBytes(path: Path, expected: ByteArray) = assert(path.readBytes().contentEquals(expected))
