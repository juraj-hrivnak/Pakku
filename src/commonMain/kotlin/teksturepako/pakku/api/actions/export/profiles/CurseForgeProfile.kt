package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.exportProfile
import teksturepako.pakku.api.actions.export.rules.cfMissingProjectsRule
import teksturepako.pakku.api.actions.export.rules.cfModpackRule
import teksturepako.pakku.api.actions.export.rules.replacementRule
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.integration.autopackdirector.autoPackDirectorRule
import teksturepako.pakku.integration.filedierector.fileDirectorRule

fun curseForgeProfile() = exportProfile(name = CurseForge.serialName, requiresPlatform = CurseForge) {
    rule { cfModpackRule() }

    optionalRule { fileDirectorRule(excludedProviders = setOf(CurseForge)) }
        .orElse { autoPackDirectorRule(excludedProviders = setOf(CurseForge)) }
        .orElse { cfMissingProjectsRule() }

    rule { replacementRule() }
}
