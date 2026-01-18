package teksturepako.pakku.api.actions.export

import strikt.api.expectThat
import strikt.assertions.isEqualTo
import teksturepako.pakku.PakkuTest
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import kotlin.test.Test

class ExportProfileDslTest : PakkuTest()
{
    fun notNullRule(): ExportRule = ExportRule { it.ignore() }
    fun nullRule(): ExportRule? = null

    @Test
    fun `orElse chain stops when first optional rule succeeds`()
    {
        val exportProfile = exportProfile("test") {
            rule { notNullRule() } // 1st rule

            optionalRule { notNullRule() }  // 2nd rule
                .orElse { nullRule() }      // skip
                .orElse { notNullRule() }   // skip
        }.build(exportRuleScope(LockFile(), ConfigFile()))

        expectThat(exportProfile.rules.size).describedAs { "exportProfile.rules.size ($this)" }.isEqualTo(2)
    }

    @Test
    fun `orElse chain adds no rules when all optional rules return null`()
    {
        val exportProfile = exportProfile("test") {
            rule { notNullRule() } // 1st rule

            optionalRule { nullRule() }  // skip
                .orElse { nullRule() }   // skip
                .orElse { nullRule() }   // skip
        }.build(exportRuleScope(LockFile(), ConfigFile()))

        expectThat(exportProfile.rules.size).describedAs { "exportProfile.rules.size ($this)" }.isEqualTo(1)
    }

    @Test
    fun `orElse chain executes until first non-null rule is found`()
    {
        val exportProfile = exportProfile("test") {
            rule { notNullRule() } // 1st rule

            optionalRule { nullRule() }     // skip
                .orElse { nullRule() }      // skip
                .orElse { notNullRule() }   // 2nd rule
                .orElse { nullRule() }      // skip
                .orElse { notNullRule() }   // skip
        }.build(exportRuleScope(LockFile(), ConfigFile()))

        expectThat(exportProfile.rules.size).describedAs { "exportProfile.rules.size ($this)" }.isEqualTo(2)
    }
}