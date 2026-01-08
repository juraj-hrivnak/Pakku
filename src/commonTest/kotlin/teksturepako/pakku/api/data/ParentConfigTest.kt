package teksturepako.pakku.api.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue
import teksturepako.pakku.PakkuTest
import kotlin.test.Test

class ParentConfigTest : PakkuTest()
{
    @Test
    fun `ParentConfig serialization with all fields`()
    {
        val config = ParentConfig(
            type = "modrinth",
            id = "fabric-example",
            version = "1.0.0",
            autoSync = true
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<ParentConfig>(json)

        expectThat(decoded) {
            get { type }.isEqualTo("modrinth")
            get { id }.isEqualTo("fabric-example")
            get { version }.isEqualTo("1.0.0")
            get { autoSync }.isTrue()
        }
    }

    @Test
    fun `ParentConfig serialization with null version tracks latest`()
    {
        val config = ParentConfig(
            type = "curseforge",
            id = "12345",
            version = null,
            autoSync = false
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<ParentConfig>(json)

        expectThat(decoded) {
            get { type }.isEqualTo("curseforge")
            get { id }.isEqualTo("12345")
            get { version }.isNull()
            get { autoSync }.isFalse()
        }
    }

    @Test
    fun `ParentConfig serialization with default autoSync`()
    {
        val config = ParentConfig(
            type = "local",
            id = "/path/to/modpack",
            version = null
        )

        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<ParentConfig>(json)

        expectThat(decoded) {
            get { type }.isEqualTo("local")
            get { id }.isEqualTo("/path/to/modpack")
            get { version }.isNull()
            get { autoSync }.isFalse()
        }
    }

    @Test
    fun `ParentConfig deserializes from JSON with SerialName`()
    {
        val json = """
            {
                "type": "modrinth",
                "id": "vanilla-plus",
                "version": "2.1.0",
                "autoSync": false
            }
        """.trimIndent()

        val config = Json.decodeFromString<ParentConfig>(json)

        expectThat(config) {
            get { type }.isEqualTo("modrinth")
            get { id }.isEqualTo("vanilla-plus")
            get { version }.isEqualTo("2.1.0")
            get { autoSync }.isFalse()
        }
    }
}
