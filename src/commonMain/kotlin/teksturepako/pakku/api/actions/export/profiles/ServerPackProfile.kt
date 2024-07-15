package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportServerPack
import teksturepako.pakku.api.actions.export.rules.replacementRule

class ServerPackProfile : ExportProfile(
    name = "serverpack",
    rules = listOf(
        exportServerPack(),
        replacementRule()
    )
)