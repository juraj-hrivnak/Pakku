package teksturepako.pakku

import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.generatePakkuId
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.pakku
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

open class PakkuTest(protected open val teardown: Boolean = true)
{
    private var testName: String = ""

    protected open suspend fun `set-up`()
    {
    }

    protected fun testFile(vararg path: String): Path
    {
        return Path(workingPath, *path)
    }

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
        pakku {
            developmentMode()
        }
        
        debugMode = true

        testName = this::class.simpleName ?: generatePakkuId()

        println("Setting up test: $testName")

        workingPath = "./build/test/$testName"
        runCatching { Path("./build/test/$testName").createParentDirectories() }
        runCatching { Path("./build/test/$testName").createDirectory() }

        runBlocking { this@PakkuTest.`set-up`() }
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