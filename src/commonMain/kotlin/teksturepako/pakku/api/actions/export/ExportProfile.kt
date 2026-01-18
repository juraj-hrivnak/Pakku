package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.debug

@DslMarker
annotation class ExportSystemDsl

/**
 * An export profile is used to contain a list of [export rules][ExportRule].
 *
 * Each export profile is independent of each other and
 * will result in one exported file.
 *
 * An export profile can be exported using the
 * [ExportProfile.export()][ExportProfile.export] extension function.
 * Multiple export profiles can be exported
 * asynchronously when used as a parameter in the [export()][export] function.
 */
open class ExportProfile(
    val name: String,
    val fileExtension: String = "zip",
    val rules: List<ExportRule?>,
    val requiresPlatform: Platform? = null
)

/**
 * Creates an export profile with customized settings and export rules.
 *
 * ```
 * val profile = exportProfile(name = "MyProfile") {
 *     rule { /* Add export rules */ }
 *     optionalRule { /* Add optional export rules */ } orElse { /* ... */ }
 * }
 * ```
 *
 * @param name The unique name for the export profile.
 * @param fileExtension The file extension for the exported profile (defaults to "zip").
 * @param requiresPlatform Optional platform constraint for the export profile.
 * @param builder A lambda function to add export rules.
 */
@ExportSystemDsl
fun exportProfile(
    name: String,
    fileExtension: String = "zip",
    requiresPlatform: Platform? = null,
    builder: ExportProfileBuilder.() -> Unit
): ExportProfileBuilder = ExportProfileBuilder(name, fileExtension, requiresPlatform, builder)

class ExportProfileBuilder(
    val name: String,
    val fileExtension: String = "zip",
    val requiresPlatform: Platform? = null,
    val builder: (ExportProfileBuilder.() -> Unit),
) : ExportRuleScope
{
    private val _rules: MutableList<ExportRule> = mutableListOf()
    val rules = _rules.toList()

    override lateinit var lockFile: LockFile
    override lateinit var configFile: ConfigFile

    fun build(exportRuleScope: ExportRuleScope): ExportProfile
    {
        this.lockFile = exportRuleScope.lockFile
        this.configFile = exportRuleScope.configFile

        this._rules.clear()
        this.apply(builder)

        debug { println("Building [${this.name} profile]") }
        debug { println("[${this.name} profile] has ${this._rules.size} rule(s)") }

        return ExportProfile(this.name, this.fileExtension, this._rules.toList(), this.requiresPlatform)
    }

    // -- AFTER BUILD --

    /** Adds an export rule to the profile. */
    @ExportSystemDsl
    fun rule(exportRule: (ExportRuleScope) -> ExportRule): ExportRule
    {
        val rule = exportRule(this)

        _rules.add(rule)
        return rule
    }

    /** Adds an optional export rule to the profile. */
    @ExportSystemDsl
    fun optionalRule(exportRule: (ExportRuleScope) -> ExportRule?): ExportRule?
    {
        val rule = exportRule(this)

        if (rule != null)
        {
            _rules.add(rule)
            return rule
        }
        else return null
    }

    /** Provides a fallback mechanism when an optional rule is null. */
    @ExportSystemDsl
    infix fun ExportRule?.orElse(exportRule: (ExportRuleScope) -> ExportRule?): ExportRule?
    {
        if (this == null)
        {
            val rule = exportRule(this@ExportProfileBuilder)

            if (rule != null)
            {
                _rules.add(rule)
                return rule
            }
            else return null
        }
        else return this
    }
}

interface ExportRuleScope
{
    /** The lock file associated with the exporting scope. */
    val lockFile: LockFile

    /** The config file associated with the exporting scope. */
    val configFile: ConfigFile
}

fun exportRuleScope(lockFile: LockFile, configFile: ConfigFile): ExportRuleScope = object : ExportRuleScope
{
    override val lockFile: LockFile = lockFile
    override val configFile: ConfigFile = configFile
}
