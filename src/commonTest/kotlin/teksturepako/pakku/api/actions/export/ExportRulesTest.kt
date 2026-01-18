package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.get
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.assertions.*
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.export.profiles.curseForgeProfile
import teksturepako.pakku.api.actions.export.profiles.modrinthProfile
import teksturepako.pakku.api.actions.import.toCfModpackModel
import teksturepako.pakku.api.actions.import.toMrModpackModel
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.models.cf.CfModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.readPathTextOrNull
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for CurseForge and Modrinth export rules with server-side mod filtering.
 * Tests the implementation of the `export_server_side_projects_to_client` configuration option.
 */
class ExportRulesTest : PakkuTest()
{
    private val modpackName = "ExportRulesTestModpack"
    private val mcVersion = "1.20.1"
    private val fabricVersion = "0.15.0"

    // Test project: Server-side mod (e.g., BlueMap)
    private val serverModCfId = 406810
    private val serverModCfFileId = 4826863
    private val serverModMrId = "swbUV1cr"
    private val serverModMrFileId = "abc123"

    private val serverMod = Project(
        type = ProjectType.MOD,
        side = ProjectSide.SERVER,
        id = mutableMapOf(
            CurseForge.serialName to serverModCfId.toString(),
            Modrinth.serialName to serverModMrId
        ),
        name = mutableMapOf(
            CurseForge.serialName to "BlueMap",
            Modrinth.serialName to "BlueMap"
        ),
        slug = mutableMapOf(
            CurseForge.serialName to "bluemap",
            Modrinth.serialName to "bluemap"
        ),
        files = mutableSetOf(
            ProjectFile(
                type = CurseForge.serialName,
                fileName = "bluemap-1.20.1-fabric.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = serverModCfFileId.toString(),
                parentId = serverModCfId.toString(),
            ),
            ProjectFile(
                type = Modrinth.serialName,
                fileName = "bluemap-1.20.1-fabric.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = serverModMrFileId,
                parentId = serverModMrId,
                url = "https://cdn.modrinth.com/data/$serverModMrId/versions/$serverModMrFileId/bluemap-1.20.1-fabric.jar",
                hashes = mutableMapOf(
                    "sha512" to "abc123def456",
                    "sha1" to "def456"
                ),
                size = 1024
            )
        )
    )

    // Test project: Client-side mod (e.g., JEI)
    private val clientModCfId = 238222
    private val clientModCfFileId = 4826999
    private val clientModMrId = "u6dRKJwZ"
    private val clientModMrFileId = "xyz789"

    private val clientMod = Project(
        type = ProjectType.MOD,
        side = ProjectSide.CLIENT,
        id = mutableMapOf(
            CurseForge.serialName to clientModCfId.toString(),
            Modrinth.serialName to clientModMrId
        ),
        name = mutableMapOf(
            CurseForge.serialName to "Just Enough Items",
            Modrinth.serialName to "Just Enough Items"
        ),
        slug = mutableMapOf(
            CurseForge.serialName to "jei",
            Modrinth.serialName to "jei"
        ),
        files = mutableSetOf(
            ProjectFile(
                type = CurseForge.serialName,
                fileName = "jei-1.20.1-fabric.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = clientModCfFileId.toString(),
                parentId = clientModCfId.toString(),
            ),
            ProjectFile(
                type = Modrinth.serialName,
                fileName = "jei-1.20.1-fabric.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = clientModMrFileId,
                parentId = clientModMrId,
                url = "https://cdn.modrinth.com/data/$clientModMrId/versions/$clientModMrFileId/jei-1.20.1-fabric.jar",
                hashes = mutableMapOf(
                    "sha512" to "xyz123abc456",
                    "sha1" to "abc789"
                ),
                size = 2048
            )
        )
    )

    // Test project: Both-side mod (e.g., Fabric API)
    private val bothModCfId = 306612
    private val bothModCfFileId = 4827100
    private val bothModMrId = "P7dR8mSH"
    private val bothModMrFileId = "both123"

    private val bothMod = Project(
        type = ProjectType.MOD,
        side = ProjectSide.BOTH,
        id = mutableMapOf(
            CurseForge.serialName to bothModCfId.toString(),
            Modrinth.serialName to bothModMrId
        ),
        name = mutableMapOf(
            CurseForge.serialName to "Fabric API",
            Modrinth.serialName to "Fabric API"
        ),
        slug = mutableMapOf(
            CurseForge.serialName to "fabric-api",
            Modrinth.serialName to "fabric-api"
        ),
        files = mutableSetOf(
            ProjectFile(
                type = CurseForge.serialName,
                fileName = "fabric-api-1.20.1.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = bothModCfFileId.toString(),
                parentId = bothModCfId.toString(),
            ),
            ProjectFile(
                type = Modrinth.serialName,
                fileName = "fabric-api-1.20.1.jar",
                mcVersions = mutableListOf("1.20.1"),
                loaders = mutableListOf("fabric"),
                id = bothModMrFileId,
                parentId = bothModMrId,
                url = "https://cdn.modrinth.com/data/$bothModMrId/versions/$bothModMrFileId/fabric-api-1.20.1.jar",
                hashes = mutableMapOf(
                    "sha512" to "both123xyz456",
                    "sha1" to "xyz456"
                ),
                size = 3072
            )
        )
    )

    // -- USER STORY 1: CurseForge Export Tests --

    @Test fun `CurseForge export excludes SERVER mods when config is false`() = runTest {

        val lockFile = LockFile(
            target = CurseForge.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(serverMod, clientMod, bothMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )

        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(curseForgeProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod should NOT be in files
        expectThat(modpackModel.files)
            .none { get { projectID }.isEqualTo(serverModCfId) }

        // CLIENT mod should be in files
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(clientModCfId) }

        // BOTH mod should be in files
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(bothModCfId) }
    }

    @Test fun `CurseForge export includes SERVER mods when config is true`() = runTest {
        runBlocking {
        val lockFile = LockFile(
            target = CurseForge.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(serverMod, clientMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.setExportServerSideProjectsToClient(true)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(curseForgeProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod SHOULD be in files (backward compatibility)
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(serverModCfId) }

        // CLIENT mod should be in files
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(clientModCfId) }
        }
    }

    @Test fun `CurseForge export includes CLIENT mods regardless of config`() = runTest {

        val lockFile = LockFile(
            target = CurseForge.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(clientMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(curseForgeProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // CLIENT mod should always be in files
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(clientModCfId) }
    }

    // -- USER STORY 2: Modrinth Export Tests --

    @Test fun `Modrinth export sets env fields correctly for SERVER mod`() = runTest {

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(serverMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.setExportServerSideProjectsToClient(true) // Include to test env fields
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find SERVER mod in files
        val serverFile = modpackModel.files.find { it.path.contains("bluemap") }
        assertNotNull(serverFile)

        // Verify env fields for SERVER mod with backward compatibility
        // When export_server_side_projects_to_client=true, SERVER mods are treated as BOTH for compatibility
        expectThat(serverFile.env?.client).isEqualTo("required")
        expectThat(serverFile.env?.server).isEqualTo("required")
    }

    @Test fun `Modrinth export sets env fields correctly for CLIENT mod`() = runTest {

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(clientMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find CLIENT mod in files
        val clientFile = modpackModel.files.find { it.path.contains("jei") }
        assertNotNull(clientFile)

        // Verify env fields for CLIENT mod
        expectThat(clientFile.env?.client).isEqualTo("required")
        expectThat(clientFile.env?.server).isEqualTo("unsupported")
    }

    @Test
    fun `test Modrinth export sets env fields correctly for BOTH mod`() = runTest {

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(bothMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )

        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find BOTH mod in files
        val bothFile = modpackModel.files.find { it.path.contains("fabric-api") }
        assertNotNull(bothFile)

        // Verify env fields for BOTH mod
        expectThat(bothFile.env?.client).isEqualTo("required")
        expectThat(bothFile.env?.server).isEqualTo("required")
    }

    @Test fun `Modrinth export includes SERVER mods with correct env when config is false`() = runTest {

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(serverMod, clientMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod SHOULD be included (Modrinth supports env fields)
        val serverFile = modpackModel.files.find { it.path.contains("bluemap") }
        assertNotNull(serverFile)
        
        // Verify env fields correctly express SERVER-only constraint
        expectThat(serverFile.env?.client).isEqualTo("unsupported")
        expectThat(serverFile.env?.server).isEqualTo("required")

        // CLIENT mod should be in files
        expectThat(modpackModel.files)
            .any { get { path }.contains("jei") }
    }

    // -- USER STORY 3: Backward Compatibility Tests --

    @Test fun `CurseForge export includes untagged mods normally`() = runTest {

        val untaggedMod = Project(
            type = ProjectType.MOD,
            side = null, // No side specified
            id = mutableMapOf(CurseForge.serialName to "999999"),
            name = mutableMapOf(CurseForge.serialName to "Untagged Mod"),
            slug = mutableMapOf(CurseForge.serialName to "untagged-mod"),
            files = mutableSetOf(
                ProjectFile(
                    type = CurseForge.serialName,
                    fileName = "untagged-mod.jar",
                    mcVersions = mutableListOf(mcVersion),
                    loaders = mutableListOf("fabric"),
                    id = "888888",
                    parentId = "999999",
                )
            )
        )

        val lockFile = LockFile(
            target = CurseForge.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(untaggedMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(curseForgeProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // Untagged mod should be included (treated as OVERRIDE)
        expectThat(modpackModel.files)
            .any { get { projectID }.isEqualTo(999999) }
    }

    @Test fun `Modrinth export sets BOTH env for untagged mods`() = runTest {

        val untaggedMod = Project(
            type = ProjectType.MOD,
            side = null, // No side specified
            id = mutableMapOf(Modrinth.serialName to "untagged"),
            name = mutableMapOf(Modrinth.serialName to "Untagged Mod"),
            slug = mutableMapOf(Modrinth.serialName to "untagged-mod"),
            files = mutableSetOf(
                ProjectFile(
                    type = Modrinth.serialName,
                    fileName = "untagged-mod.jar",
                    mcVersions = mutableListOf(mcVersion),
                    loaders = mutableListOf("fabric"),
                    id = "untagged123",
                    parentId = "untagged",
                    url = "https://cdn.modrinth.com/data/untagged/versions/untagged123/untagged-mod.jar",
                    hashes = mutableMapOf("sha512" to "untagged123", "sha1" to "untagged456"),
                    size = 4096
                )
            )
        )

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(untaggedMod)
        )

        val configFile = ConfigFile(
            name = modpackName
        )
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find untagged mod in files
        val untaggedFile = modpackModel.files.find { it.path.contains("untagged") }
        assertNotNull(untaggedFile)

        // Verify env fields for untagged mod (should be BOTH)
        expectThat(untaggedFile.env?.client).isEqualTo("required")
        expectThat(untaggedFile.env?.server).isEqualTo("required")
    }
}
