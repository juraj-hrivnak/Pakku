package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ExportEffectsTest : PakkuTest(debug = false)
{
    @Test
    fun `cache hits succeed without resolving content`() = runBlocking {
        val context = context("cache-hit")
        val outputPath = context.getPath("mods", "cached.jar")
        outputPath.createParentDirectories()
        outputPath.writeBytes(byteArrayOf(1))
        var resolved = false

        val paths = listOf(context.createFile(
            bytesCallback = {
                resolved = true
                Ok(byteArrayOf(2))
            },
            path = "mods",
            subpath = arrayOf("cached.jar"),
        )).runEffects { fail(it.rawMessage) }.awaitAll()

        assertFalse(resolved)
        assertEquals(listOf(outputPath), paths)
        assertEquals(byteArrayOf(1).toList(), outputPath.toFile().readBytes().toList())
    }

    @Test
    fun `file actions only wait for the same output path`() = runBlocking {
        val context = context("ordering")
        val sharedPath = context.getPath("shared")
        val independentPath = context.getPath("independent")
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val firstFinished = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val independentStarted = CompletableDeferred<Unit>()

        val results = listOf(
            context.ruleResult("first", Packaging.FileAction(sharedPath) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                firstFinished.complete(Unit)
                sharedPath to null
            }),
            context.ruleResult("second", Packaging.FileAction(sharedPath) {
                assertTrue(firstFinished.isCompleted)
                secondStarted.complete(Unit)
                sharedPath to null
            }),
            context.ruleResult("independent", Packaging.FileAction(independentPath) {
                independentStarted.complete(Unit)
                independentPath to null
            }),
        )

        val export = async { results.runEffects { fail(it.rawMessage) }.awaitAll() }
        firstStarted.await()
        withTimeout(5_000) { independentStarted.await() }
        assertFalse(secondStarted.isCompleted)

        releaseFirst.complete(Unit)
        export.await()
        assertTrue(secondStarted.isCompleted)
    }

    private fun context(subdir: String) = RuleContext.MissingProject(
        project = Project(
            type = ProjectType.MOD,
            slug = mutableMapOf("test" to "test"),
            name = mutableMapOf("test" to "Test"),
            id = mutableMapOf("test" to "test"),
            files = mutableSetOf(),
        ),
        lockFile = LockFile(),
        configFile = ConfigFile(),
        workingSubDir = subdir,
    )
}
