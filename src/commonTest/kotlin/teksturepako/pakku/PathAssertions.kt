package teksturepako.pakku

import strikt.api.Assertion
import java.nio.file.Path
import kotlin.io.path.*

fun Assertion.Builder<Path>.exists() = assert("exists") {
    if (it.exists()) pass(actual = "exists") else fail(actual = "does not exist")
}

fun Assertion.Builder<Path>.doesNotExist() = assert("does not exist") {
    if (!it.exists()) pass(actual = "does not exist") else fail(actual = "exists")
}

fun Assertion.Builder<Path>.isDirectory() = assert("is directory") {
    if (it.isDirectory()) pass(actual = "is directory") else fail(actual = "is file")
}

fun Assertion.Builder<Path>.isFile() = assert("is file") {
    if (it.isRegularFile()) pass(actual = "is file") else fail(actual = "is directory")
}

fun Assertion.Builder<Path>.hasContent(expected: String) = assert("has content %s", expected) {
    when (val actual = it.readText())
    {
        expected -> pass()
        else     -> fail(actual)
    }
}

fun Assertion.Builder<Path>.hasBytes(expected: ByteArray) = assert("has bytes") {
    val actual = it.readBytes()
    if (actual.contentEquals(expected)) pass()
    else fail("Expected ${expected.size} bytes, got ${actual.size} bytes")
}
