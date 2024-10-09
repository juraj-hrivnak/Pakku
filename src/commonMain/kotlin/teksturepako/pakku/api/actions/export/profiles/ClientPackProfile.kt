package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportClientPack
import teksturepako.pakku.api.actions.export.rules.exportServerPack
import teksturepako.pakku.api.actions.export.rules.replacementRule

class ClientPackProfile : ExportProfile(
    name = "clientpack",
    rules = listOf(
        exportClientPack(),
        replacementRule()
    )
)