package teksturepako.pakku.api.actions

import com.github.ajalt.mordant.terminal.Terminal
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.cli.arg.overrideYes
import teksturepako.pakku.cli.resolveDependencies
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdditionTest
{
    @Test
    fun `parent project with the same variant is not added again`() = runTest {
        val parentProject = project("parent.jar")
        val effectiveLock = LockFile().apply { add(parentProject) }
        var alreadyAdded = false
        var accepted = false

        project("parent.jar").createAdditionRequest(
            onError = { alreadyAdded = it is AlreadyAdded },
            onSuccess = { _, _, _, _ -> accepted = true },
            lockFile = effectiveLock,
            platforms = listOf(Modrinth),
        )

        assertTrue(alreadyAdded)
        assertFalse(accepted)
    }

    @Test
    fun `different parent variant enters replacement flow`() = runTest {
        val parentProject = project("parent.jar")
        val effectiveLock = LockFile().apply { add(parentProject) }
        var replacement: Project? = null

        project("fork.jar").createAdditionRequest(
            onError = {},
            onSuccess = { _, _, replacing, _ -> replacement = replacing },
            lockFile = effectiveLock,
            platforms = listOf(Modrinth),
        )

        assert(replacement == parentProject)
    }

    @Test
    fun `different parent dependency variant can replace parent`() = runTest {
        val root = project("root", "root.jar")
        val parentDependency = project("dependency", "parent.jar")
        val requestedDependency = project("dependency", "fork.jar")
        val local = LockFile().apply { add(root) }
        val effective = LockFile().apply { add(parentDependency) }

        overrideYes = true
        try
        {
            root.resolveDependencies(
                terminal = Terminal(),
                reqHandlers = RequestHandlers(onError = {}, onSuccess = { _, _, _, _ -> }),
                lockFile = local,
                projectProvider = Modrinth,
                platforms = listOf(Modrinth),
                effectiveLockFile = effective,
                onDependencyReq = { _, _, _ -> listOf(Ok(requestedDependency)) },
            )
        }
        finally
        {
            overrideYes = false
        }

        assert(local.getProject(requestedDependency)?.files == requestedDependency.files)
    }

    private fun project(fileName: String) = project("example-mod", fileName)

    private fun project(slug: String, fileName: String) = Project(
        type = ProjectType.MOD,
        slug = mutableMapOf(Modrinth.serialName to slug),
        name = mutableMapOf(Modrinth.serialName to slug),
        id = mutableMapOf(Modrinth.serialName to slug),
        files = mutableSetOf(ProjectFile(type = Modrinth.serialName, fileName = fileName, id = fileName)),
    )
}
