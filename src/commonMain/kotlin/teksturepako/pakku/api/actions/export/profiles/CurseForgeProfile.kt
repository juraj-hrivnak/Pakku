package teksturepako.pakku.api.actions.export.profiles

import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.rules.createCfModpackModel
import teksturepako.pakku.api.actions.export.rules.ruleOfCfModpack
import teksturepako.pakku.api.actions.export.rules.ruleOfCfMissingProjects
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.compat.exportFileDirector

class CurseForgeProfile(lockFile: LockFile, configFile: ConfigFile) : ExportProfile(
    name = CurseForge.serialName,
    rules = listOf(
        lockFile.getFirstMcVersion()?.let {
            createCfModpackModel(it, lockFile, configFile)
        }?.let { ruleOfCfModpack(it) },

        if (lockFile.getAllProjects().any { "filedirector" in it })
        {
            exportFileDirector(Modrinth)
        }
        else
        {
            ruleOfCfMissingProjects(Modrinth)
        }
    )
)
