package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.exportServerPack

class ServerPackProfile : ExportProfile(
    name = "serverpack",
    rules = listOf(
        exportServerPack()
    )
)