package teksturepako.pakku

import strikt.api.expectThat
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.fail

// DSL for creating test structure with path tracking
fun PakkuTest.testStructure(block: StructureBuilder.() -> Unit): TestStructure =
    StructureBuilder(this).apply(block).build()

// Unified builder for creating test structures
class StructureBuilder(
    private val test: PakkuTest,
    private val basePath: Array<String> = emptyArray(),
    private val paths: MutableMap<List<String>, Path> = mutableMapOf(),
)
{
    private fun resolvePath(vararg parts: String): Array<String> = basePath + parts

    private fun withPath(name: String, action: (Array<String>) -> Unit): Path
    {
        val fullPath = resolvePath(name)
        action(fullPath)
        return test.testPath(*fullPath).also { path ->
            paths[fullPath.toList()] = path
        }
    }

    fun dir(name: String, block: StructureBuilder.() -> Unit = {})
    {
        withPath(name) { parts -> test.createTestDir(*parts) }
        StructureBuilder(test, resolvePath(name), paths).apply(block)
    }

    fun file(name: String)
    {
        withPath(name) { parts -> test.createTestFile(*parts) }
    }

    fun file(name: String, content: String?)
    {
        withPath(name) { parts ->
            test.createTestFile(*parts)
            content?.let { test.testPath(*parts).writeText(it) }
        }
    }

    fun file(name: String, content: ByteArray?)
    {
        withPath(name) { parts ->
            test.createTestFile(*parts)
            content?.let { test.testPath(*parts).writeBytes(it) }
        }
    }

    fun build() = TestStructure(test, paths)
}

// TestStructure with path access
class TestStructure(
    val test: PakkuTest,
    private val paths: Map<List<String>, Path>,
)
{
    private fun findPath(pathParts: List<String>, predicate: (Path) -> Boolean, type: String): Path = paths
        .filter { predicate(it.value) }
        .let { filtered ->
            filtered[pathParts] ?: fail(
                "$type '${pathParts.joinToString("/")}' not found in test structure. " + "Available ${type.lowercase()}s: ${
                    filtered.keys.map { it.joinToString("/") }.sorted()
                }"
            )
        }

    fun file(vararg path: String): Path = findPath(path.toList(), Path::isRegularFile, "File")
    fun dir(vararg path: String): Path = findPath(path.toList(), Path::isDirectory, "Directory")

    val allPaths: Set<List<String>> get() = paths.keys
}

// DSL for verifying structure
fun PakkuTest.expectStructure(block: StructureExpectation.() -> Unit) = StructureExpectation(this).apply(block)

// Unified expectation builder
class StructureExpectation(
    private val test: PakkuTest,
    private val basePath: Array<String> = emptyArray(),
)
{
    private fun resolvePath(vararg parts: String): Array<String> = basePath + parts

    private fun resolveFilePath(name: String): Path = test.testPath(*resolvePath(name))

    private fun verifyPath(name: String, block: (Path) -> Unit)
    {
        resolveFilePath(name).also(block)
    }

    fun dir(name: String, block: StructureExpectation.() -> Unit = {})
    {
        verifyPath(name) { path ->
            expectThat(path).exists()
            expectThat(path).isDirectory()
        }
        StructureExpectation(test, resolvePath(name)).apply(block)
    }

    fun file(name: String)
    {
        verifyPath(name) { path ->
            expectThat(path).exists()
            expectThat(path).isFile()
        }
    }

    fun file(name: String, content: String?)
    {
        verifyPath(name) { path ->
            expectThat(path).exists()
            expectThat(path).isFile()
            content?.let { expectThat(path).hasContent(it) }
        }
    }

    fun file(name: String, content: ByteArray?)
    {
        verifyPath(name) { path ->
            expectThat(path).exists()
            expectThat(path).isFile()
            content?.let { expectThat(path).hasBytes(it) }
        }
    }

    fun doesNotExist(name: String)
    {
        verifyPath(name) { path ->
            expectThat(path).doesNotExist()
        }
    }
}