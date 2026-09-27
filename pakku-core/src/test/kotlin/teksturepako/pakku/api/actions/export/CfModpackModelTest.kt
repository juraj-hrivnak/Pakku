package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.export.profiles.curseForgeProfile
import teksturepako.pakku.api.actions.import.toCfModpackModel
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.readPathTextFromZip
import teksturepako.pakku.io.readPathTextOrNull
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertNotNull

class CfModpackModelTest : PakkuTest()
{
    private val exportDeps = mockk<ExportDeps> {
        coEvery { resolveContent(any()) } returns Ok(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
    }

    private val modpackName = "CurseForgeProfileTestModpack"

    private val greeneryCfId = 574029
    private val greeneryCfFileId = 5913357

    private val greeneryProject = Project(
        type = ProjectType.MOD,
        id = mutableMapOf(CurseForge.serialName to greeneryCfId.toString()),
        name = mutableMapOf(CurseForge.serialName to "Greenery\uD83C\uDF3F"),
        slug = mutableMapOf(CurseForge.serialName to "greenery"),
        files = mutableSetOf(
            ProjectFile(
                type = CurseForge.serialName,
                fileName = "Greenery-1.12.2-7.0.jar",
                mcVersions = mutableListOf("1.12.2", "1.12.1", "1.12"),
                loaders = mutableListOf("forge"),
                id = greeneryCfFileId.toString(),
                parentId = greeneryCfId.toString(),
            )
        )
    )

    private val mcVersion = "1.12.2"
    private val forgeVersion = "xxx.xxx.xxx"

    override suspend fun `set-up`()
    {
        val lockFile = LockFile(
            target = CurseForge.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("forge" to forgeVersion),
            projects = mutableListOf(greeneryProject)
        )

        val configFile = ConfigFile(
            name = modpackName
        )

        val platforms = lockFile.getPlatforms().get()

        assertNotNull(platforms)

        export(
            profiles = listOf(curseForgeProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = exportDeps,
        )
    }

    @Test
    fun `test cf modpack model in cache`() = runTest {
        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)

        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    @Test
    fun `test cf modpack model in zip`() = runTest {
        val zipPath = testPath("build", CurseForge.serialName, "$modpackName.${CfModpackModel.EXTENSION}")

        val modpackModel = readPathTextFromZip(zipPath, CfModpackModel.MANIFEST).toCfModpackModel()

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    private fun testModpackModel(modpackModel: CfModpackModel)
    {
        assert(CfModpackModel.CfModData(greeneryCfId, greeneryCfFileId) in modpackModel.files)
        assert(modpackModel.name == modpackName)
        assert(modpackModel.minecraft.version == mcVersion)
        assert(modpackModel.minecraft.modLoaders.firstOrNull()?.id == "forge-$forgeVersion")
    }
}