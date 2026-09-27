package teksturepako.pakku.api.data

import kotlinx.coroutines.test.runTest
import com.github.michaelbull.result.get
import teksturepako.pakku.PakkuTest
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ConfigFile configuration migration functionality.
 * Tests the `export_server_side_projects_to_client` field migration logic
 * based on lockfile version.
 */
class ConfigFileTest : PakkuTest()
{
    @Test
    fun `test lockfile v1 triggers migration to true`() = runTest {
        // Create a config file (simulating old project)
        val configFile = ConfigFile(
            name = "Test Modpack",
            version = "1.0.0"
        )
        configFile.write()
        
        // Create a lockfile with version 1 (simulating old project from JSON)
        // Note: Default is now v2, so we write v1 to JSON directly to simulate old project
        Path("$workingPath/${LockFile.FILE_NAME}").writeText("""{"lockfile_version":1}""")
        val lockFile = LockFile.readOrNew().get()!!
        assert(lockFile.getLockFileVersion() == 1)
        
        // Trigger migration
        val (migratedConfig, migratedLockFile, wasMigrated) = 
            ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
        
        // Verify migration occurred
        assertTrue(wasMigrated)
        
        // Verify migration set the field to true for backward compatibility
        assertTrue(migratedConfig.getExportServerSideProjectsToClient())
        
        // Verify lockfile version was bumped to 2
        assert(migratedLockFile.getLockFileVersion() == 2)
    }

    @Test
    fun `test lockfile v2 does not trigger migration`() = runTest {
        // Create a config file
        val configFile = ConfigFile(
            name = "Test Modpack",
            version = "1.0.0"
        )
        configFile.write()
        
        // Create a lockfile with version 2 (new or already migrated project)
        // Note: Default is now v2, so just use LockFile()
        val lockFile = LockFile()
        assert(lockFile.getLockFileVersion() == 2)
        lockFile.write()
        
        // Trigger migration
        val (migratedConfig, migratedLockFile, wasMigrated) = 
            ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
        
        // Should not migrate (lockfile is already v2)
        assertFalse(wasMigrated)
        
        // Should use default false
        assertFalse(migratedConfig.getExportServerSideProjectsToClient())
        
        // Lockfile version should remain 2
        assert(migratedLockFile.getLockFileVersion() == 2)
    }

    @Test
    fun `test migration is idempotent with lockfile version`() = runTest {
        // Create config and write it
        val configFile = ConfigFile(
            name = "Test Modpack",
            version = "1.0.0"
        )
        configFile.write()
        
        // Create a lockfile v1 (simulating old project from JSON)
        Path("$workingPath/${LockFile.FILE_NAME}").writeText("""{"lockfile_version":1}""")
        val lockFile = LockFile.readOrNew().get()!!
        
        // First migration
        val (firstConfig, firstLockFile, firstMigrated) = 
            ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
        assertTrue(firstMigrated)
        assertTrue(firstConfig.getExportServerSideProjectsToClient())
        assert(firstLockFile.getLockFileVersion() == 2)
        
        // Second migration attempt with v2 lockfile
        val (secondConfig, secondLockFile, secondMigrated) = 
            ConfigFile.readOrNull()!!.migrateIfNeeded(firstLockFile)
        
        // Should not migrate again
        assertFalse(secondMigrated)
        assert(secondLockFile.getLockFileVersion() == 2)
    }

    @Test
    fun `test new project uses default false`() = runTest {
        // Simulate new project with lockfile v2
        val configFile = ConfigFile.readOrNew().get()!!
        configFile.setName("New Modpack")
        configFile.setVersion("0.0.1")
        configFile.write()
        
        // New project should have lockfile v2 (default)
        val lockFile = LockFile()
        assert(lockFile.getLockFileVersion() == 2)
        lockFile.write()
        
        // Read back and verify default is false
        val readConfig = ConfigFile.readOrNull()!!
        assertFalse(readConfig.getExportServerSideProjectsToClient())
    }

    @Test
    fun `test config with true value persists correctly`() = runTest {
        // Create config with explicit true
        val configFile = ConfigFile(
            name = "Test Modpack",
            version = "1.0.0"
        )
        configFile.setExportServerSideProjectsToClient(true)
        configFile.write()
        
        // Read back
        val readConfig = ConfigFile.readOrNull()!!
        assertTrue(readConfig.getExportServerSideProjectsToClient())
    }

    @Test
    fun `test config with false value persists correctly`() = runTest {
        // Create config with explicit false
        val configFile = ConfigFile(
            name = "Test Modpack",
            version = "1.0.0"
        )
        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()
        
        // Read back
        val readConfig = ConfigFile.readOrNull()!!
        assertFalse(readConfig.getExportServerSideProjectsToClient())
    }

    @Test
    fun `test new project init with default lockfile v2 does not trigger migration`() = runTest {
        // Simulate Init command behavior:
        // 1. Create config with exportServerSideProjectsToClient = false
        val configFile = ConfigFile.readOrNew().get()!!
        configFile.setName("New Modpack")
        configFile.setVersion("0.0.1")
        configFile.setExportServerSideProjectsToClient(false)
        configFile.write()

        // 2. Create lockfile (default is now v2, no need to bump)
        val lockFile = LockFile()
        assert(lockFile.getLockFileVersion() == 2)
        lockFile.write()

        // 3. Simulate first export - should NOT trigger migration
        val (migratedConfig, migratedLockFile, wasMigrated) =
            ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)

        // Verify no migration occurred
        assertFalse(wasMigrated)

        // Verify exportServerSideProjectsToClient remains false (not overwritten to true)
        assertFalse(migratedConfig.getExportServerSideProjectsToClient())

        // Verify lockfile version remains 2
        assert(migratedLockFile.getLockFileVersion() == 2)
    }
}
