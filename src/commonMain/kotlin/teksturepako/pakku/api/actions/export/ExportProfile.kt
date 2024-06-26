package teksturepako.pakku.api.actions.export

/**
 * An export profile is used to contain a list of [export rules][ExportRule].
 *
 * Each export profile is independent of each other and
 * will result in one exported file.
 */
open class ExportProfile(
    val name: String,
    val fileExtension: String = "zip",
    val rules: List<ExportRule?>
)
