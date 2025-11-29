package teksturepako.pakku.api.export

import com.github.michaelbull.result.get
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.export.export
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

        runBlocking {
            export(
                profiles = listOf(curseForgeProfile()),
                onError = { _, _ -> },
                onSuccess = { _, _, _ -> },
                lockFile, configFile, platforms
            )
        }
    }

    @Test
    fun `test cf modpack model in cache`()
    {
        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)

        val modpackModel = runBlocking {
            readPathTextOrNull(manifestPath).toCfModpackModel()
        }

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    @Test
    fun `test cf modpack model in zip`()
    {
        val zipPath = testPath("build", CurseForge.serialName, "$modpackName.${CfModpackModel.EXTENSION}")

        val modpackModel = runBlocking {
            readPathTextFromZip(zipPath, CfModpackModel.MANIFEST).toCfModpackModel()
        }

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    private fun testModpackModel(modpackModel: CfModpackModel)
    {
        expectThat(modpackModel.files)
            .contains(CfModpackModel.CfModData(greeneryCfId, greeneryCfFileId))

        expectThat(modpackName)
            .isEqualTo(modpackModel.name)

        expectThat(mcVersion)
            .isEqualTo(modpackModel.minecraft.version)

        expectThat("forge-$forgeVersion")
            .isEqualTo(modpackModel.minecraft.modLoaders.firstOrNull()?.id)
    }
}