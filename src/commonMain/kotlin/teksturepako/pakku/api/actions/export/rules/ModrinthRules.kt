package teksturepako.pakku.api.actions.export.rules

import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.api.actions.export.ExportRuleScope
import teksturepako.pakku.api.actions.export.Packaging
import teksturepako.pakku.api.actions.export.RuleContext.*
import teksturepako.pakku.api.actions.export.ruleResult
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.api.models.mr.MrModpackModel
import teksturepako.pakku.api.models.mr.MrModpackModel.MrFile
import teksturepako.pakku.api.overrides.OverrideType
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Modrinth
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.io.createHash
import teksturepako.pakku.io.readPathBytesOrNull

fun ExportRuleScope.mrModpackRule(): ExportRule
{
    val modpackModel = lockFile.getFirstMcVersion()?.run {
        createMrModpackModel(this, lockFile, configFile)
    }

    /**
     * Read configuration option for server-side project export.
     * Unlike CurseForge, Modrinth supports env fields, so we include all projects
     * and use env fields to express environment constraints.
     */
    val exportServerSide = configFile.getExportServerSideProjectsToClient() ?: true

    return ExportRule {
        when (it)
        {
            is ExportingProject         ->
            {
                // When clientOnly is enabled, skip SERVER-only projects (server-overrides)
                if (it.clientOnly)
                {
                    val overrideType = OverrideType.fromProject(it.project)
                    if (overrideType == OverrideType.SERVER_OVERRIDE)
                    {
                        return@ExportRule it.ignore()
                    }
                }

                val projectFile = it.project.getFilesForProviders(Modrinth, GitHub).firstOrNull()
                    ?: return@ExportRule it.setMissing()

                // Modrinth: Include all projects, env fields will handle environment constraints
                it.addToMrModpackModel(projectFile, modpackModel ?: return@ExportRule it.error(RequiresMcVersion))
            }
            is ExportingOverride       ->
            {
                // When clientOnly is enabled, exclude server-overrides
                val allowedTypes = if (it.clientOnly) {
                    setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
                } else {
                    // Use configured behavior for override files when not in clientOnly mode
                    if (!exportServerSide) {
                        setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
                    } else {
                        null // null means all types are allowed
                    }
                }
                it.export(allowedTypes = allowedTypes)
            }
            is ExportingManualOverride ->
            {
                // When clientOnly is enabled, exclude server-overrides
                val allowedTypes = if (it.clientOnly) {
                    setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
                } else {
                    // Use configured behavior for override files when not in clientOnly mode
                    if (!exportServerSide) {
                        setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE)
                    } else {
                        null // null means all types are allowed
                    }
                }
                it.export(allowedTypes = allowedTypes)
            }
            is Finished                ->
            {
                it.createJsonFile(modpackModel, MrModpackModel.MANIFEST, format = jsonEncodeDefaults)
            }
            else                        -> it.ignore()
        }
    }
}

fun mrMissingProjectsRule() = ExportRule {
    when (it)
    {
        is MissingProject ->
        {
            it.exportAsOverride(excludedProviders = setOf(Modrinth, GitHub)) { bytesCallback, fileName, overridesDir ->
                it.createFile(bytesCallback, overridesDir, it.project.getPathStringWithSubpath(it.configFile), fileName)
            }
        }
        else -> it.ignore()
    }
}

fun ExportingProject.addToMrModpackModel(projectFile: ProjectFile, modpackModel: MrModpackModel) =
    ruleResult("addToMrModpackModel ${project.type} ${project.slug}", Packaging.Action {
        projectFile.toMrFile(configFile, this.project)?.let { mrFile ->
            modpackModel.files.add(mrFile)
        }
        null // Return no error
    })

fun getMrLoaderName(loader: String) = when (loader)
{
    "fabric" -> "fabric-loader"
    "quilt"  -> "quilt-loader"
    else     -> loader
}

fun createMrModpackModel(
    mcVersion: String,
    lockFile: LockFile,
    configFile: ConfigFile,
): MrModpackModel
{
    val mrDependencies = mapOf("minecraft" to mcVersion) + (lockFile.getLoadersWithVersions().firstOrNull()
        ?.let { (loaderName, loaderVersion) -> mapOf(getMrLoaderName(loaderName) to loaderVersion) }
        ?: mapOf())

    return MrModpackModel(
        name = configFile.getName(),
        summary = configFile.getDescription(),
        versionId = configFile.getVersion(),
        dependencies = mrDependencies
    )
}

suspend fun ProjectFile.toMrFile(configFile: ConfigFile, parentProject: Project): MrFile?
{
    if (this.type !in listOf(Modrinth.serialName, GitHub.serialName)) return null

    /**
     * MUST NOT contain un-encoded spaces or any other illegal characters according to RFC 3986.
     * [Source...](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack#h_e2af55e39e)
     */
    val url = UrlEncoderUtil.encode(this.url ?: return null, allow = "/:")

    val relativePathString = this.getRelativePathString(parentProject, configFile)
    val path = this.getPath(parentProject, configFile)

    /**
     * Map project side to Modrinth env fields based on OverrideType and configuration.
     * 
     * Modrinth supports native environment fields, allowing precise control over mod loading.
     * 
     * When `export_server_side_projects_to_client = false` (correct behavior):
     * - SERVER_OVERRIDE: client="unsupported", server="required" (follows official side constraints)
     * - CLIENT_OVERRIDE: client="required", server="unsupported"
     * - OVERRIDE (BOTH/null): client="required", server="required"
     * 
     * When `export_server_side_projects_to_client = true` (backward compatibility):
     * - SERVER_OVERRIDE: client="required", server="required" (treated as BOTH for compatibility)
     * - CLIENT_OVERRIDE: client="required", server="unsupported"
     * - OVERRIDE (BOTH/null): client="required", server="required"
     */
    val exportServerSide = configFile.getExportServerSideProjectsToClient() ?: true
    val overrideType = OverrideType.fromProject(parentProject)
    val env = when (overrideType)
    {
        OverrideType.SERVER_OVERRIDE -> MrFile.Env(
            // When exportServerSide=false, properly set client="unsupported" to follow side constraints
            // When exportServerSide=true, set client="required" for backward compatibility (treat as BOTH)
            client = if (exportServerSide) "required" else "unsupported",
            server = "required"
        )
        OverrideType.CLIENT_OVERRIDE -> MrFile.Env(
            client = "required",
            server = "unsupported"
        )
        OverrideType.OVERRIDE -> MrFile.Env(
            client = "required",
            server = "required"
        )
    }

    return MrFile(
        path = relativePathString,
        hashes = MrFile.Hashes(
            sha512 = this.hashes?.get("sha512")
                ?: readPathBytesOrNull(path)?.let { bytes ->
                    createHash("sha512", bytes)
                } ?: return null,
            sha1 = this.hashes?.get("sha1")
                ?: readPathBytesOrNull(path)?.let { bytes ->
                    createHash("sha1", bytes)
                } ?: return null,
        ),
        env = env,
        downloads = setOf(url),
        fileSize = this.size
    )
}