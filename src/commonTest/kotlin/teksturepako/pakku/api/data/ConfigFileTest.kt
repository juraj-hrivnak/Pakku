package teksturepako.pakku.api.data

import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import teksturepako.pakku.PakkuTest
import kotlin.test.Test

/**
 * Tests for ConfigFile configuration migration functionality.
 * Tests the `export_server_side_projects_to_client` field migration logic
 * based on lockfile version.
 */
class ConfigFileTest : PakkuTest()
{
    @Test
    fun `test lockfile v1 triggers migration to true`()
    {
        runBlocking {
            // Create a config file (simulating old project)
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.write()
            
            // Create a lockfile with version 1 (old project)
            val lockFile = LockFile()
            expectThat(lockFile.getLockFileVersion()).isEqualTo(1)
            lockFile.write()
            
            // Trigger migration
            val (migratedConfig, migratedLockFile, wasMigrated) = 
                ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
            
            // Verify migration occurred
            expectThat(wasMigrated).isTrue()
            
            // Verify migration set the field to true for backward compatibility
            expectThat(migratedConfig.getExportServerSideProjectsToClient()).isEqualTo(true)
            
            // Verify lockfile version was bumped to 2
            expectThat(migratedLockFile.getLockFileVersion()).isEqualTo(2)
        }
    }

    @Test
    fun `test lockfile v2 does not trigger migration`()
    {
        runBlocking {
            // Create a config file
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.write()
            
            // Create a lockfile with version 2 (new or already migrated project)
            val lockFile = LockFile().bumped() // Bump to version 2
            expectThat(lockFile.getLockFileVersion()).isEqualTo(2)
            lockFile.write()
            
            // Trigger migration
            val (migratedConfig, migratedLockFile, wasMigrated) = 
                ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
            
            // Should not migrate (lockfile is already v2)
            expectThat(wasMigrated).isFalse()
            
            // Should use default false
            expectThat(migratedConfig.getExportServerSideProjectsToClient()).isEqualTo(false)
            
            // Lockfile version should remain 2
            expectThat(migratedLockFile.getLockFileVersion()).isEqualTo(2)
        }
    }

    @Test
    fun `test migration is idempotent with lockfile version`()
    {
        runBlocking {
            // Create config and lockfile v1
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.write()
            
            val lockFile = LockFile()
            lockFile.write()
            
            // First migration
            val (firstConfig, firstLockFile, firstMigrated) = 
                ConfigFile.readOrNull()!!.migrateIfNeeded(lockFile)
            expectThat(firstMigrated).isTrue()
            expectThat(firstConfig.getExportServerSideProjectsToClient()).isEqualTo(true)
            expectThat(firstLockFile.getLockFileVersion()).isEqualTo(2)
            
            // Second migration attempt with v2 lockfile
            val (secondConfig, secondLockFile, secondMigrated) = 
                ConfigFile.readOrNull()!!.migrateIfNeeded(firstLockFile)
            
            // Should not migrate again
            expectThat(secondMigrated).isFalse()
            expectThat(secondLockFile.getLockFileVersion()).isEqualTo(2)
        }
    }

    @Test
    fun `test new project uses default false`()
    {
        runBlocking {
            // Simulate new project with lockfile v2
            val configFile = ConfigFile.readOrNew()
            configFile.setName("New Modpack")
            configFile.setVersion("0.0.1")
            configFile.write()
            
            // New project should have lockfile v2
            val lockFile = LockFile().bumped()
            lockFile.write()
            
            // Read back and verify default is false
            val readConfig = ConfigFile.readOrNull()!!
            expectThat(readConfig.getExportServerSideProjectsToClient()).isEqualTo(false)
        }
    }

    @Test
    fun `test config with true value persists correctly`()
    {
        runBlocking {
            // Create config with explicit true
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.setExportServerSideProjectsToClient(true)
            configFile.write()
            
            // Read back
            val readConfig = ConfigFile.readOrNull()!!
            expectThat(readConfig.getExportServerSideProjectsToClient()).isEqualTo(true)
        }
    }

    @Test
    fun `test config with false value persists correctly`()
    {
        runBlocking {
            // Create config with explicit false
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.setExportServerSideProjectsToClient(false)
            configFile.write()
            
            // Read back
            val readConfig = ConfigFile.readOrNull()!!
            expectThat(readConfig.getExportServerSideProjectsToClient()).isEqualTo(false)
        }
    }

    @Test
    fun `test new project init with bumped lockfile does not trigger migration`()
    {
        runBlocking {
            // Simulate Init command behavior:
            // 1. Create config with exportServerSideProjectsToClient = false
            val configFile = ConfigFile.readOrNew()
            configFile.setName("New Modpack")
            configFile.setVersion("0.0.1")
            configFile.setExportServerSideProjectsToClient(false)
            configFile.write()

            // 2. Create lockfile and bump version (as Init now does)
            val lockFile = LockFile()
            val bumpedLockFile = lockFile.bumped()
            expectThat(bumpedLockFile.getLockFileVersion()).isEqualTo(2)
            bumpedLockFile.write()

            // 3. Simulate first export - should NOT trigger migration
            val (migratedConfig, migratedLockFile, wasMigrated) =
                ConfigFile.readOrNull()!!.migrateIfNeeded(bumpedLockFile)

            // Verify no migration occurred
            expectThat(wasMigrated).isFalse()

            // Verify exportServerSideProjectsToClient remains false (not overwritten to true)
            expectThat(migratedConfig.getExportServerSideProjectsToClient()).isEqualTo(false)

            // Verify lockfile version remains 2
            expectThat(migratedLockFile.getLockFileVersion()).isEqualTo(2)
        }
    }
}
