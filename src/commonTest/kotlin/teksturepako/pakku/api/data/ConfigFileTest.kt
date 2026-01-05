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
 * Tests the `export_server_side_projects_to_client` field migration logic.
 */
class ConfigFileTest : PakkuTest()
{
    @Test
    fun `test config migration adds field with true for existing projects`()
    {
        runBlocking {
            // Create a config file without the new field (simulating old project)
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            
            // Verify field is initially null
            expectThat(configFile.getExportServerSideProjectsToClient()).isEqualTo(null)
            
            // Write the config
            configFile.write()
            
            // Read it back and trigger migration
            val (migratedConfig, wasMigrated) = ConfigFile.readOrNull()!!.migrateIfNeeded()
            
            // Verify migration occurred
            expectThat(wasMigrated).isTrue()
            
            // Verify migration added the field with value true
            expectThat(migratedConfig.getExportServerSideProjectsToClient()).isEqualTo(true)
        }
    }

    @Test
    fun `test config migration is idempotent`()
    {
        runBlocking {
            // Create a config file and trigger migration
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.write()
            
            val (firstMigration, firstWasMigrated) = ConfigFile.readOrNull()!!.migrateIfNeeded()
            expectThat(firstWasMigrated).isTrue()
            expectThat(firstMigration.getExportServerSideProjectsToClient()).isEqualTo(true)
            
            // Write again and migrate again
            firstMigration.write()
            val (secondMigration, secondWasMigrated) = ConfigFile.readOrNull()!!.migrateIfNeeded()
            
            // Should not migrate again (idempotent)
            expectThat(secondWasMigrated).isEqualTo(false)
            
            // Should still be true (not modified twice)
            expectThat(secondMigration.getExportServerSideProjectsToClient()).isEqualTo(true)
            
            // Both should be equal
            expectThat(firstMigration.getExportServerSideProjectsToClient())
                .isEqualTo(secondMigration.getExportServerSideProjectsToClient())
        }
    }

    @Test
    fun `test config migration does not modify existing field`()
    {
        runBlocking {
            // Create a config file with the field already set to false
            val configFile = ConfigFile(
                name = "Test Modpack",
                version = "1.0.0"
            )
            configFile.setExportServerSideProjectsToClient(false)
            configFile.write()
            
            // Trigger migration
            val (migratedConfig, wasMigrated) = ConfigFile.readOrNull()!!.migrateIfNeeded()
            
            // Should not have migrated (field already exists)
            expectThat(wasMigrated).isEqualTo(false)
            
            // Should still be false (not changed to true)
            expectThat(migratedConfig.getExportServerSideProjectsToClient()).isEqualTo(false)
        }
    }

    @Test
    fun `test pakku init creates config with false default`()
    {
        runBlocking {
            // Simulate init command creating a new config
            val configFile = ConfigFile.readOrNew()
            configFile.setName("New Modpack")
            configFile.setVersion("0.0.1")
            
            // Set the default as Init.kt does
            configFile.setExportServerSideProjectsToClient(false)
            
            configFile.write()
            
            // Read back and verify
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
}
