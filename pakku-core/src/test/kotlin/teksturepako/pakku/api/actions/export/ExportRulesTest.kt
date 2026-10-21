package teksturepako.pakku.api.actions.export

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.HashMismatch
import teksturepako.pakku.api.actions.errors.NoHashes
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
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readPathTextOrNull
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// TODO: Clean up this test
/**
 * Tests for CurseForge and Modrinth export rules with server-side mod filtering.
 * Tests the implementation of the `export_server_side_projects_to_client` configuration option.
 */
class ExportRulesTest : PakkuTest()
{
    private val exportDeps = mockk<ExportDeps> {
        coEvery { resolveContent(any()) } returns Ok(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
    }

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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod should NOT be in files
        assert(modpackModel.files.none { it.projectID == serverModCfId })

        // CLIENT mod should be in files
        assert(modpackModel.files.any { it.projectID == clientModCfId })

        // BOTH mod should be in files
        assert(modpackModel.files.any { it.projectID == bothModCfId })
    }

    @Test fun `CurseForge export includes SERVER mods when config is true`() = runTest {

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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod SHOULD be in files (backward compatibility)
        assert(modpackModel.files.any { it.projectID == serverModCfId })

        // CLIENT mod should be in files
        assert(modpackModel.files.any { it.projectID == clientModCfId })
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // CLIENT mod should always be in files
        assert(modpackModel.files.any { it.projectID == clientModCfId })
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find SERVER mod in files
        val serverFile = modpackModel.files.find { it.path.contains("bluemap") }
        assertNotNull(serverFile)

        // Verify env fields for SERVER mod with backward compatibility
        // When export_server_side_projects_to_client=true, SERVER mods are treated as BOTH for compatibility
        assert(serverFile.env?.client == "required")
        assert(serverFile.env?.server == "required")
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find CLIENT mod in files
        val clientFile = modpackModel.files.find { it.path.contains("jei") }
        assertNotNull(clientFile)

        // Verify env fields for CLIENT mod
        assert(clientFile.env?.client == "required")
        assert(clientFile.env?.server == "unsupported")
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find BOTH mod in files
        val bothFile = modpackModel.files.find { it.path.contains("fabric-api") }
        assertNotNull(bothFile)

        // Verify env fields for BOTH mod
        assert(bothFile.env?.client == "required")
        assert(bothFile.env?.server == "required")
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // SERVER mod SHOULD be included (Modrinth supports env fields)
        val serverFile = modpackModel.files.find { it.path.contains("bluemap") }
        assertNotNull(serverFile)
        
        // Verify env fields correctly express SERVER-only constraint
        assert(serverFile.env?.client == "unsupported")
        assert(serverFile.env?.server == "required")

        // CLIENT mod should be in files
        assert(modpackModel.files.any { "jei" in it.path })
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, CurseForge.serialName, CfModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toCfModpackModel()

        assertNotNull(modpackModel)

        // Untagged mod should be included (treated as OVERRIDE)
        assert(modpackModel.files.any { it.projectID == 999999 })
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
            lockFile, configFile, platforms,
            deps = exportDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()

        assertNotNull(modpackModel)

        // Find untagged mod in files
        val untaggedFile = modpackModel.files.find { it.path.contains("untagged") }
        assertNotNull(untaggedFile)

        // Verify env fields for untagged mod (should be BOTH)
        assert(untaggedFile.env?.client == "required")
        assert(untaggedFile.env?.server == "required")
    }

    // -- GitHub Export Tests (issue #157) --

    private val githubPayload = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte())

    private val githubMod = Project(
        type = ProjectType.MOD,
        side = ProjectSide.BOTH,
        id = mutableMapOf(GitHub.serialName to "Metallurgists-of-Create/Create-TFMG-CE"),
        name = mutableMapOf(GitHub.serialName to "Create TFMG CE"),
        slug = mutableMapOf(GitHub.serialName to "Create-TFMG-CE"),
        files = mutableSetOf(
            ProjectFile(
                type = GitHub.serialName,
                fileName = "create-tfmg-ce-1.0.0.jar",
                mcVersions = mutableListOf(mcVersion),
                loaders = mutableListOf("fabric"),
                url = "https://github.com/Metallurgists-of-Create/Create-TFMG-CE/releases/download/v1.0.0/create-tfmg-ce-1.0.0.jar",
                hashes = mutableMapOf(
                    "sha256" to createHash("sha256", githubPayload)
                ),
                size = githubPayload.size.toInt()
            )
        )
    )

    @Test
    fun `GitHub download fallback - manifest contains entry with computed hashes`() = runTest {
        val testDeps = ExportDeps { Ok(githubPayload) }

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(githubMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()
        assertNotNull(modpackModel)

        val githubFile = modpackModel.files.find { it.path.contains("create-tfmg-ce") }
        assertNotNull(githubFile)
        assertEquals(createHash("sha512", githubPayload), githubFile.hashes.sha512)
        assertEquals(createHash("sha1", githubPayload), githubFile.hashes.sha1)
        assert(githubFile.downloads.any { it.contains("github.com") })
    }

    @Test
    fun `GitHub export hermeticity - dev environment is ignored`() = runTest {
        val differentPayload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val testDeps = ExportDeps { Ok(githubPayload) }

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(githubMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        // Create a dev-env file with different content — it should be ignored
        createTestFile("mods", "create-tfmg-ce-1.0.0.jar")
        val devEnvPath = testPath("mods", "create-tfmg-ce-1.0.0.jar")
        devEnvPath.writeBytes(differentPayload)

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()
        assertNotNull(modpackModel)

        val githubFile = modpackModel.files.find { it.path.contains("create-tfmg-ce") }
        assertNotNull(githubFile)
        // Hashes should match the downloaded payload, NOT the dev-env file
        assertEquals(createHash("sha512", githubPayload), githubFile.hashes.sha512)
        assertEquals(createHash("sha1", githubPayload), githubFile.hashes.sha1)
    }

    @Test
    fun `Lock-file hashes short-circuit - no download occurs`() = runTest {
        val downloadCalled = kotlinx.atomicfu.atomic(false)
        val lockFileHashesMod = Project(
            type = ProjectType.MOD,
            side = ProjectSide.BOTH,
            id = mutableMapOf(GitHub.serialName to "owner/repo"),
            name = mutableMapOf(GitHub.serialName to "LockHashesMod"),
            slug = mutableMapOf(GitHub.serialName to "lock-hashes-mod"),
            files = mutableSetOf(
                ProjectFile(
                    type = GitHub.serialName,
                    fileName = "lock-hashes-mod.jar",
                    mcVersions = mutableListOf(mcVersion),
                    loaders = mutableListOf("fabric"),
                    url = "https://github.com/owner/repo/releases/download/v1/lock-hashes-mod.jar",
                    hashes = mutableMapOf(
                        "sha512" to "abc123fromlockfile",
                        "sha1" to "def456fromlockfile"
                    ),
                    size = 1024
                )
            )
        )

        val testDeps = ExportDeps {
            downloadCalled.value = true
            Ok(githubPayload)
        }

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(lockFileHashesMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, _ -> },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()
        assertNotNull(modpackModel)

        val modFile = modpackModel.files.find { it.path.contains("lock-hashes-mod") }
        assertNotNull(modFile)
        assertEquals("abc123fromlockfile", modFile.hashes.sha512)
        assertEquals("def456fromlockfile", modFile.hashes.sha1)

        // No download should occur when both hashes are in the lock file
        assert(!downloadCalled.value)
    }

    @Test
    fun `Download failure is visible - error reported to onError`() = runTest {
        val downloadError: ActionError = HashMismatch(null, "expected", "actual")
        val testDeps = ExportDeps { Err(downloadError) }

        var receivedError: ActionError? = null

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(githubMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, error -> receivedError = error },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        // Error should be reported
        assertNotNull(receivedError)
        assertEquals(downloadError, receivedError)

        // The entry should be absent from the manifest
        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()
        assertNotNull(modpackModel)
        assertNull(modpackModel.files.find { it.path.contains("create-tfmg-ce") })
    }

    @Test
    fun `Integrity mismatch - HashMismatch reported`() = runTest {
        // Create a GitHub project where the lock file sha256 does NOT match the downloaded bytes
        val wrongSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        val integrityMismatchMod = Project(
            type = ProjectType.MOD,
            side = ProjectSide.BOTH,
            id = mutableMapOf(GitHub.serialName to "owner/integrity-test"),
            name = mutableMapOf(GitHub.serialName to "IntegrityTest"),
            slug = mutableMapOf(GitHub.serialName to "integrity-test"),
            files = mutableSetOf(
                ProjectFile(
                    type = GitHub.serialName,
                    fileName = "integrity-test.jar",
                    mcVersions = mutableListOf(mcVersion),
                    loaders = mutableListOf("fabric"),
                    url = "https://github.com/owner/integrity-test/releases/download/v1/integrity-test.jar",
                    hashes = mutableMapOf("sha256" to wrongSha256),
                    size = githubPayload.size.toInt()
                )
            )
        )

        val testDeps = ExportDeps { Ok(githubPayload) }

        var receivedError: ActionError? = null

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(integrityMismatchMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, error -> receivedError = error },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        // HashMismatch should be reported
        assertNotNull(receivedError)
        assert(receivedError is HashMismatch)
    }

    @Test
    fun `NoHashes parity with fetch - entry added and warning reported`() = runTest {
        // Create a GitHub project with NO hashes at all (hashes = null)
        val noHashesMod = Project(
            type = ProjectType.MOD,
            side = ProjectSide.BOTH,
            id = mutableMapOf(GitHub.serialName to "owner/no-hashes"),
            name = mutableMapOf(GitHub.serialName to "NoHashesMod"),
            slug = mutableMapOf(GitHub.serialName to "no-hashes-mod"),
            files = mutableSetOf(
                ProjectFile(
                    type = GitHub.serialName,
                    fileName = "no-hashes-mod.jar",
                    mcVersions = mutableListOf(mcVersion),
                    loaders = mutableListOf("fabric"),
                    url = "https://github.com/owner/no-hashes/releases/download/v1/no-hashes-mod.jar",
                    hashes = null, // No hashes at all
                    size = githubPayload.size.toInt()
                )
            )
        )

        val testDeps = ExportDeps { Ok(githubPayload) }

        var receivedError: ActionError? = null

        val lockFile = LockFile(
            target = Modrinth.serialName,
            mcVersions = mutableListOf(mcVersion),
            loaders = mutableMapOf("fabric" to fabricVersion),
            projects = mutableListOf(noHashesMod)
        )

        val configFile = ConfigFile(name = modpackName)
        configFile.write()

        val platforms = lockFile.getPlatforms().get()
        assertNotNull(platforms)

        export(
            profiles = listOf(modrinthProfile()),
            onError = { _, error -> receivedError = error },
            onSuccess = { _, _, _ -> },
            lockFile, configFile, platforms,
            deps = testDeps,
        )

        // NoHashes warning should be reported (fetch parity)
        assertNotNull(receivedError)
        assert(receivedError is NoHashes)

        // The entry should STILL be added to the manifest (warn-and-continue)
        val manifestPath = Path(Dirs.cacheDir.pathString, Modrinth.serialName, MrModpackModel.MANIFEST)
        val modpackModel = readPathTextOrNull(manifestPath).toMrModpackModel()
        assertNotNull(modpackModel)
        val modFile = modpackModel.files.find { it.path.contains("no-hashes-mod") }
        assertNotNull(modFile)
        // Hashes should be computed from the downloaded bytes
        assertEquals(createHash("sha512", githubPayload), modFile.hashes.sha512)
        assertEquals(createHash("sha1", githubPayload), modFile.hashes.sha1)
    }
}
