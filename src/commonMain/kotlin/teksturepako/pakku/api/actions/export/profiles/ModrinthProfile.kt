package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.exportProfile
import teksturepako.pakku.api.actions.export.rules.mrMissingProjectsRule
import teksturepako.pakku.api.actions.export.rules.mrModpackRule
import teksturepako.pakku.api.actions.export.rules.replacementRule
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.integration.fileDirectorRule

fun modrinthProfile() = exportProfile(
    name = Modrinth.serialName, fileExtension = "mrpack", requiresPlatform = Modrinth
) {
    rule { mrModpackRule() }
    optionalRule { fileDirectorRule(excludedProviders = setOf(Modrinth)) } orElse { mrMissingProjectsRule() }
    rule { replacementRule() }
}
