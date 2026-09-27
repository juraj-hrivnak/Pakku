package teksturepako.pakku.api.actions.update

import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import kotlin.test.Test

class UpdateTest : PakkuTest(debug = false)
{
    private fun fabricApiProject(vararg files: ProjectFile) = Project(
        type = ProjectType.MOD,
        slug = mutableMapOf(Modrinth.serialName to "fabric-api"),
        name = mutableMapOf(Modrinth.serialName to "Fabric API"),
        id = mutableMapOf(Modrinth.serialName to "P7dR5mHq"),
        files = files.toMutableSet(),
    )

    private fun mockMrFile(mcVersion: String, published: Instant, fileId: String) =
        mockk<ProjectFile> {
            every { type } returns Modrinth.serialName
            every { fileName } returns "fabric-api-$mcVersion.jar"
            every { mcVersions } returns mutableListOf(mcVersion)
            every { loaders } returns mutableListOf("fabric")
            every { datePublished } returns published
            every { id } returns fileId
        }

    @Test
    fun `prefer mc version higher in lock file`()
    {
        val older = Instant.parse("2024-06-01T00:00:00Z")
        val newer = Instant.parse("2024-12-01T00:00:00Z")

        val file1211 = mockMrFile("1.21.1", newer, "file-1211")
        val file1214 = mockMrFile("1.21.4", older, "file-1214")

        val accProject = fabricApiProject(file1211)
        val newProject = fabricApiProject(file1211, file1214)

        val updated = combineProjects(
            accProject = accProject,
            newProject = newProject,
            platformName = Modrinth.serialName,
            numberOfFiles = 1,
            mcVersions = listOf("1.21.4", "1.21.1"),
        )

        assert(updated.getLatestFile(listOf(Modrinth))!!.mcVersions == mutableListOf("1.21.4"))
    }

    @Test
    fun `prefer mc version listed first in lock file`()
    {
        val older = Instant.parse("2024-06-01T00:00:00Z")
        val newer = Instant.parse("2024-12-01T00:00:00Z")

        val file1211 = mockMrFile("1.21.1", older, "file-1211")
        val file1214 = mockMrFile("1.21.4", newer, "file-1214")

        val accProject = fabricApiProject(file1211)
        val newProject = fabricApiProject(file1211, file1214)

        val updated = combineProjects(
            accProject = accProject,
            newProject = newProject,
            platformName = Modrinth.serialName,
            numberOfFiles = 1,
            mcVersions = listOf("1.21.1"),
        )

        assert(updated.getLatestFile(listOf(Modrinth))!!.mcVersions == mutableListOf("1.21.1"))
    }
}
