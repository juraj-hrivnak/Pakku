package teksturepako.pakku.api.actions.export

import teksturepako.pakku.api.actions.export.rules.ExportRule

data class ExportProfile(
    val name: String,
    val fileExtension: String = "zip",
    val rules: List<ExportRule?>
)
