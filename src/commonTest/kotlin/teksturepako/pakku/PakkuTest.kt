package teksturepako.pakku

import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.generatePakkuId
import teksturepako.pakku.api.data.workingPath
import kotlin.io.path.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class PakkuTest
{
    private var testName: String = ""

    protected open suspend fun `on-set-up`()
    {
    }

    protected open val teardown = true

    protected fun createTestFile(vararg path: String)
    {
        val file = Path(workingPath, *path)
        runCatching { file.createParentDirectories() }
        runCatching { file.createFile() }
    }

    protected fun createTestDir(vararg path: String)
    {
        val dir = Path(workingPath, *path)
        runCatching { dir.createParentDirectories() }
        runCatching { dir.createDirectory() }
    }

    @BeforeTest
    fun `set-up-test`()
    {
        testName = this::class.simpleName ?: generatePakkuId()

        println("Setting up test: $testName")

        workingPath = "./build/test/$testName"
        runCatching { Path("./build/test/$testName").createParentDirectories() }
        runCatching { Path("./build/test/$testName").createDirectory() }

        runBlocking { this@PakkuTest.`on-set-up`() }
    }

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun `tear-down-test`()
    {
        if (!teardown) return

        workingPath = "."

        println("Tearing down test: $testName")

        runCatching { Path("./build/test/$testName").deleteRecursively() }
    }
}