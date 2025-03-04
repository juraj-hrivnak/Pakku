package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.exportProfile
import teksturepako.pakku.api.actions.export.rules.replacementRule
import teksturepako.pakku.api.actions.export.rules.serverPackRule

fun serverPackProfile() = exportProfile(name = "serverpack") {
    rule { serverPackRule() }
    rule { replacementRule() }
}
