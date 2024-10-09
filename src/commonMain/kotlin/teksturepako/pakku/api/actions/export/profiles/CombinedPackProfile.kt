package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportCombinedPack
import teksturepako.pakku.api.actions.export.rules.replacementRule

class CombinedPackProfile : ExportProfile(
    name = "combinedpack",
    rules = listOf(
        exportCombinedPack(),
        replacementRule()
    )
)