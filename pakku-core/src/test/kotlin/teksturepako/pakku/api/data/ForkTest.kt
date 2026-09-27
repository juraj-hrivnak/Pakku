package teksturepako.pakku.api.data

import com.github.michaelbull.result.get
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
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

        assert(parent.mergedWithLocal(local, ConfigFile()).getAllProjects() == listOf(localProject))
    }

    @Test
    fun `parent override paths retain the parent checkout as their root`() = runTest {
        val parent = testPath("parent").also { it.createDirectories() }
        parent.resolve("config").also { it.createDirectories() }.resolve("base.cfg").writeText("base")
        val config = ConfigFile().apply { addOverride("config") }

        val source = getOverridesAsyncFrom(parent, config).awaitAll().single()

        assert(source.root == parent)
        assert(source.path == "config/base.cfg")
    }

    @Test
    fun `effective fork lock includes parent projects without changing local lock`() = runTest {
        Dirs.parentDir.createDirectories()
        val parentProject = project("parent-id", "parent-slug")
        val parent = LockFile().apply { add(parentProject) }
        writeToFile(parent, Dirs.parentDir.resolve(LockFile.FILE_NAME).toString(), overrideText = true)
        ConfigFile(parent = ConfigFile.ParentConfig(id = "test")).write()
        val local = LockFile()

        assert(local.withForkParent().get()!!.getAllProjects() == listOf(parentProject))
        assert(local.getAllProjects() == emptyList<Project>())
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

        assert(gitHeadTags(repository) == listOf("v1.0.0"))
    }

    private fun project(id: String, slug: String) = Project(
        type = ProjectType.MOD,
        id = mutableMapOf("provider" to id),
        name = mutableMapOf("provider" to slug),
        slug = mutableMapOf("provider" to slug),
        files = mutableSetOf(),
    )
}
