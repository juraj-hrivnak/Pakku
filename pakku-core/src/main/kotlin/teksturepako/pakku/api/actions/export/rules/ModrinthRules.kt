package teksturepako.pakku.api.actions.export.rules

import com.github.michaelbull.result.*
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.CouldNotComputeHashes
import teksturepako.pakku.api.actions.errors.HashMismatch
import teksturepako.pakku.api.actions.errors.NoHashes
import teksturepako.pakku.api.actions.errors.NoUrl
import teksturepako.pakku.api.actions.export.ExportRule
import teksturepako.pakku.io.IllegalPath
import kotlin.io.path.Path
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
import teksturepako.pakku.io.createHash
fun ExportRuleScope.mrModpackRule(): ExportRule
{
    val modpackModel = lockFile.getFirstMcVersion()?.run {
        createMrModpackModel(this, lockFile, configFile)
    }

    return ExportRule {
        when (it)
        {
            is ExportingProject         ->
            {
                if (it.noServer && OverrideType.fromProject(it.project) == OverrideType.SERVER_OVERRIDE)
                {
                    return@ExportRule it.ignore()
                }

                val projectFile = it.project.getFilesForProviders(Modrinth, GitHub).firstOrNull()
                    ?: return@ExportRule it.setMissing()

                it.addToMrModpackModel(projectFile, modpackModel
                    ?: return@ExportRule it.error(RequiresMcVersion))
            }
            is ExportingOverride       ->
            {
                if (it.noServer)
                {
                    return@ExportRule it.export(allowedTypes = setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE))
                }

                it.export()
            }
            is ExportingManualOverride ->
            {
                if (it.noServer)
                {
                    return@ExportRule it.export(allowedTypes = setOf(OverrideType.OVERRIDE, OverrideType.CLIENT_OVERRIDE))
                }

                it.export()
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
        val sha512 = projectFile.hashes?.get("sha512")
        val sha1 = projectFile.hashes?.get("sha1")

        var warning: ActionError? = null
        val bytes: ByteArray? = if (sha512 != null && sha1 != null) null
        else
        {
            val remote = deps.resolveContent(projectFile)
                ?: return@Action CouldNotComputeHashes(projectFile)

            val downloaded = remote.fold(
                success = { it },
                failure = { return@Action it }, // surface DownloadFailed
            )

        when (val integrity = projectFile.checkIntegrity(downloaded, Path(projectFile.fileName)))
            {
                is HashMismatch -> return@Action integrity
                is NoHashes -> warning = integrity
                else -> { }
            }

            downloaded
        }

        // Build the manifest entry from lock hashes and/or the downloaded bytes
        projectFile.toMrFile(configFile, this.project, bytes).fold(
            success = { modpackModel.files.add(it); warning },
            failure = { it }, // report to runEffects → onError → CLI
        )
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

fun ProjectFile.toMrFile(
    configFile: ConfigFile,
    parentProject: Project,
    bytes: ByteArray? = null,
): Result<MrFile, ActionError>
{
    if (this.type !in listOf(Modrinth.serialName, GitHub.serialName))
        return Err(IllegalPath(this.fileName))

    /**
     * MUST NOT contain un-encoded spaces or any other illegal characters according to RFC 3986.
     * [Source...](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack#h_e2af55e39e)
     */
    val url = UrlEncoderUtil.encode(this.url ?: return Err(NoUrl(this)), allow = "/:")

    val relativePathString = this.getRelativePathString(parentProject, configFile)
        ?: return Err(IllegalPath(this.fileName))

    val env = when (OverrideType.fromProject(parentProject))
    {
        OverrideType.SERVER_OVERRIDE -> MrFile.Env(
            client = if (configFile.getExportServerSideProjectsToClient()) "required" else "unsupported",
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

    val sha512 = this.hashes?.get("sha512")
    val sha1 = this.hashes?.get("sha1")

    if ((sha512 == null || sha1 == null) && bytes == null)
        return Err(CouldNotComputeHashes(this))

    return Ok(MrFile(
        path = relativePathString,
        hashes = MrFile.Hashes(
            sha512 = sha512 ?: createHash("sha512", bytes!!),
            sha1 = sha1 ?: createHash("sha1", bytes!!),
        ),
        env = env,
        downloads = setOf(url),
        fileSize = this.size
    ))
}