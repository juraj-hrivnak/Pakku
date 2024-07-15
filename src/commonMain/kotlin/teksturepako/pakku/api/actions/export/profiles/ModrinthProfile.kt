package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.createMrModpackModel
import teksturepako.pakku.api.actions.export.rules.replacementRule
import teksturepako.pakku.api.actions.export.rules.ruleOfMrMissingProjects
import teksturepako.pakku.api.actions.export.rules.ruleOfMrModpack
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.compat.exportFileDirector

class ModrinthProfile(lockFile: LockFile, configFile: ConfigFile) : ExportProfile(
    name = Modrinth.serialName,
    fileExtension = "mrpack",
    rules = listOf(
        lockFile.getFirstMcVersion()?.let {
            val modpackModel = createMrModpackModel(it, lockFile, configFile)
            ruleOfMrModpack(modpackModel)
        },

        if (lockFile.getAllProjects().any { "filedirector" in it })
        {
            exportFileDirector(CurseForge)
        }
        else ruleOfMrMissingProjects(CurseForge),

        replacementRule()
    ),
    dependsOn = Modrinth
)
