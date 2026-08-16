package teksturepako.pakku.api.data

import com.github.michaelbull.result.get
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.overrides.getOverridesAsyncFrom
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.integration.git.gitHeadTags
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import teksturepako.pakku.io.writeToFile
import kotlin.test.Test

class ForkTest : PakkuTest()
{
    @Test
    fun `local project replaces parent through stable project identity`()
    {
        val parent = LockFile().apply { add(project("shared-id", "parent-slug")) }
        val localProject = project("shared-id", "local-slug")
        val local = LockFile().apply { add(localProject) }

        expectThat(parent.mergedWithLocal(local, ConfigFile()).getAllProjects())
            .containsExactly(localProject)
    }

    @Test
    fun `parent override paths retain the parent checkout as their root`(): Unit = runBlocking {
        val parent = testPath("parent").also { it.createDirectories() }
        parent.resolve("config").also { it.createDirectories() }.resolve("base.cfg").writeText("base")
        val config = ConfigFile().apply { addOverride("config") }

        val source = getOverridesAsyncFrom(parent, config).awaitAll().single()

        expectThat(source.root).isEqualTo(parent)
        expectThat(source.path).isEqualTo("config/base.cfg")
    }

    @Test
    fun `effective fork lock includes parent projects without changing local lock`(): Unit = runBlocking {
        Dirs.parentDir.createDirectories()
        val parentProject = project("parent-id", "parent-slug")
        val parent = LockFile().apply { add(parentProject) }
        writeToFile(parent, Dirs.parentDir.resolve(LockFile.FILE_NAME).toString(), overrideText = true)
        ConfigFile(parent = ConfigFile.ParentConfig(id = "test")).write()
        val local = LockFile()

        expectThat(local.withForkParent().get()!!.getAllProjects()).containsExactly(parentProject)
        expectThat(local.getAllProjects()).containsExactly()
    }

    @Test
    fun `head tags include annotated tags resolving to head`()
    {
        val repository = testPath("tagged").also { it.createDirectories() }
        Git.init().setDirectory(repository.toFile()).call().use { git ->
            repository.resolve("file.txt").writeText("content")
            git.add().addFilepattern("file.txt").call()
            git.commit().setMessage("initial").setAuthor("Pakku", "pakku@example.invalid").call()
            git.tag().setName("v1.0.0").setMessage("release").call()
        }

        expectThat(gitHeadTags(repository)).containsExactly("v1.0.0")
    }

    private fun project(id: String, slug: String) = Project(
        type = ProjectType.MOD,
        id = mutableMapOf("provider" to id),
        name = mutableMapOf("provider" to slug),
        slug = mutableMapOf("provider" to slug),
        files = mutableSetOf(),
    )
}
