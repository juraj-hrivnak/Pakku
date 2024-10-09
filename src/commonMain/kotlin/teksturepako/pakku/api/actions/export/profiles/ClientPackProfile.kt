package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportClientPack
import teksturepako.pakku.api.actions.export.rules.replacementRule

class ClientPackProfile : ExportProfile(
    name = NAME,
    rules = listOf(exportClientPack(), replacementRule())
)
{
    companion object
    {
        const val NAME = "clientpack"

        init
        {
            all[NAME] = { _, _ -> ClientPackProfile() }
        }
    }
}