package teksturepako.pakku.api.export

import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.export.export
import teksturepako.pakku.api.actions.export.profiles.modrinthProfile
import teksturepako.pakku.api.actions.export.rules.toMrFile
import teksturepako.pakku.api.actions.import.toMrModpackModel
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.readPathTextFromZip
import teksturepako.pakku.io.readPathTextOrNull
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MrModpackModelTest : PakkuTest()
{
    private val modpackName = "ModrinthProfileTestModpack"

    private val greeneryMrId = "EVaCo3rr"
    private val greeneryMrFileId = "cjlG1S17"

    private val greeneryProject = Project(
        type = ProjectType.MOD,
        id = mutableMapOf(Modrinth.serialName to greeneryMrId),
        name = mutableMapOf(Modrinth.serialName to "Greenery\uD83C\uDF3F"),
        slug = mutableMapOf(Modrinth.serialName to "greenery"),
        files = mutableSetOf(
            ProjectFile(
                type = Modrinth.serialName,
                fileName = "Greenery-1.12.2-7.0.jar",
                mcVersions = mutableListOf("1.12.2", "1.12.1", "1.12"),
                loaders = mutableListOf("forge"),
                url = "https://cdn.modrinth.com/data/EVaCo3rr/versions/cjlG1S17/Greenery-1.12.2-7.0.jar",
                id = greeneryMrFileId,
                parentId = greeneryMrId,
                hashes = mutableMapOf(
                    "sha512" to "e398c5eea25b0b4397c2cd041dd6ea3d34c397c5b25a619070f6141cdc219fe44a2b4c21f04529cc61cc84c5eb79bbe3ef7aa570b4360b97f624ebc19413fa53",
                    "sha1" to "287bd7b023edaeac6313bfc264f99c60d7ae3ad3"
                ),
                size = 481352,
            )
        )
    )

    private val mcVersion = "1.12.2"
    private val forgeVersion = "xxx.xxx.xxx"

    private val lockFile = LockFile(
        target = Modrinth.serialName,
        mcVersions = mutableListOf(mcVersion),
        loaders = mutableMapOf("forge" to forgeVersion),
        projects = mutableListOf(greeneryProject)
    )

    private val configFile = ConfigFile(
        name = modpackName
    )

    private val platforms = lockFile.getPlatforms().getOrNull()

    override suspend fun `set-up`()
    {
        assertNotNull(platforms)

        runBlocking {
            export(
                profiles = listOf(modrinthProfile()),
                onError = { _, _ -> },
                onSuccess = { _, _, _ -> },
                lockFile, configFile, platforms
            )
        }
    }

    @Test
    fun `test mr modpack model in cache`()
    {
        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)

        val modpackModel = runBlocking {
            readPathTextOrNull(manifestPath).toMrModpackModel()
        }

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    @Test
    fun `test mr modpack model in zip`()
    {
        val zipPath = testFile("build", Modrinth.serialName, "$modpackName.${MrModpackModel.EXTENSION}")

        val modpackModel = runBlocking {
            readPathTextFromZip(zipPath, MrModpackModel.MANIFEST).toMrModpackModel()
        }

        assertNotNull(modpackModel)

        testModpackModel(modpackModel)
    }

    private fun testModpackModel(modpackModel: MrModpackModel)
    {
        val greeneryMrFile = runBlocking {
            greeneryProject.getLatestFile(listOf(Modrinth))?.toMrFile(lockFile, configFile)
        }

        assertNotNull(greeneryMrFile)

        expectThat(greeneryMrFile)
            .isNotNull()

        expectThat(modpackModel.files)
            .contains(greeneryMrFile)

        expectThat(modpackModel.name)
            .isEqualTo(modpackName)

        expectThat(modpackModel.dependencies["minecraft"])
            .isEqualTo(mcVersion)

        expectThat(modpackModel.dependencies["forge"])
            .isEqualTo(forgeVersion)
    }
}