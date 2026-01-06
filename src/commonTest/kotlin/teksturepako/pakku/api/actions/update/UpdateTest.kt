package teksturepako.pakku.api.actions.update

import com.github.ajalt.clikt.testing.test
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.cli.cmd.Add
import teksturepako.pakku.cli.cmd.Update
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class UpdateTest : PakkuTest(debug = false)
{
    @Test
    fun `prefer higher mc version`() = runTest {
        LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf("1.21.1"),
            loaders = mutableMapOf("fabric" to "")
        ).apply { write()?.onError { fail(it.message()) } }

        Add().test("fabric-api").also { println(it.output) }

        val fabricApi = LockFile.readToResult().getOrElse { fail(it.message()) }
            .getProject("fabric-api")

        assertNotNull(fabricApi)
        assertNotNull(fabricApi.getLatestFile(listOf(Modrinth))?.mcVersions)

        println("fabric-api mc versions: " + fabricApi.getLatestFile(listOf(Modrinth))?.mcVersions)

        expectThat(fabricApi.getLatestFile(listOf(Modrinth))!!.mcVersions)
            .contains("1.21.1")

        val updatedLockFile = LockFile.readToResult()
            .onFailure { fail(it.message()) }
            .get()
            ?.apply { setMcVersions(listOf("1.21.4", "1.21.1")) }
            ?.apply { write()?.onError { fail(it.message()) } }

        assertNotNull(updatedLockFile)

        println("updated mc versions: " + updatedLockFile.getMcVersions())

        Update().test("--all").also { println(it.output) }

        val updatedFabricApi = LockFile.readToResult().getOrElse { fail(it.message()) }
            .getProject("fabric-api")

        assertNotNull(updatedFabricApi)
        assertNotNull(updatedFabricApi.getLatestFile(listOf(Modrinth))?.mcVersions)

        println("updated fabric-api mc versions: " + updatedFabricApi.getLatestFile(listOf(Modrinth))?.mcVersions)

        expectThat(updatedFabricApi.getLatestFile(listOf(Modrinth))!!.mcVersions)
            .isEqualTo(mutableListOf("1.21.4"))
    }

}