package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.platforms.Platform

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
    val dependsOn: Platform? = null
)
