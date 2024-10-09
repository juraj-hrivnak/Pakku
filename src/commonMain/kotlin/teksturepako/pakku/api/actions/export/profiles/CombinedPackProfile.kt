package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportCombinedPack
import teksturepako.pakku.api.actions.export.rules.replacementRule

class CombinedPackProfile : ExportProfile(
    name = NAME,
    rules = listOf(
        exportCombinedPack(),
        replacementRule()
    )
)
{
    companion object
    {
        const val NAME = "combinedpack"

        init
        {
            all[NAME] = { _, _ -> CombinedPackProfile() }
        }
    }
}