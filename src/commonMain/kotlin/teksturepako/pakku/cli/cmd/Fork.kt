package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.Dirs
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.parentConfigFilePath
import teksturepako.pakku.api.data.parentLockFilePath
import teksturepako.pakku.api.data.sha256
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.arg.ynPrompt
import teksturepako.pakku.cli.ui.pDanger
import teksturepako.pakku.cli.ui.pError
import teksturepako.pakku.cli.ui.pMsg
import teksturepako.pakku.cli.ui.pSuccess
import teksturepako.pakku.integration.git.gitFetchCheckout
import teksturepako.pakku.integration.git.gitClone
import teksturepako.pakku.integration.git.gitHeadCommit
import teksturepako.pakku.integration.git.gitIsClean
import teksturepako.pakku.integration.git.gitRefType
import teksturepako.pakku.integration.git.gitRemoteUrl
import teksturepako.pakku.integration.git.gitSetRemoteUrl
import teksturepako.pakku.integration.git.gitUpdate
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Fork : CliktCommand()
{
    override fun help(context: Context) = "Manage a forked parent modpack"

    init
    {
        subcommands(ForkInit(), ForkSync(), ForkShow(), ForkUnset(), ForkPromote(), ForkExclude(), ForkInclude())
    }

    override fun run() = Unit
}

private class ForkInit : CliktCommand(name = "init")
{
    override fun help(context: Context) = "Initialize a fork from a parent Git repository"

    private val gitUrlOpt: String? by option("--git-url").help("Git URL of the parent repository")
    private val fromCurrentFlag: Boolean by option("--from-current").help("Use the current repository's remote as parent").flag()
    private val fromPathOpt: String? by option("--from-path").help("Use an existing local repository as parent")
    private val refNameOpt: String by option("--ref-name").help("Branch, tag or commit to track").default("main")
    private val refTypeOpt: String? by option("--ref-type").choice("branch", "tag", "commit").help("Type of ref to track")
    private val remoteOpt: String by option("--remote").help("Remote name").default("origin")

    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        if (config.parent != null)
        {
            terminal.pDanger("Parent already configured: ${config.parent?.id}")
            return@runBlocking
        }

        val parentDir = Dirs.parentDir
        if (parentDir.exists() && parentDir.listDirectoryEntries().isNotEmpty())
        {
            terminal.pDanger("Parent directory is not empty: ${parentDir.pathString}")
            return@runBlocking
        }

        val sourceUrl = when
        {
            fromPathOpt != null ->
            {
                val source = Path(fromPathOpt!!)
                if (!source.exists() || !source.isDirectory())
                {
                    terminal.pDanger("Provided path is not a directory: $source")
                    return@runBlocking
                }
                val isClean = runCatching { gitIsClean(source) }.getOrElse {
                    terminal.pDanger("Provided path is not a Git repository: $source")
                    return@runBlocking
                }
                if (!isClean)
                {
                    terminal.pDanger("Local repository at --from-path has uncommitted changes")
                    return@runBlocking
                }
                gitRemoteUrl(source, remoteOpt) ?: run {
                    terminal.pDanger("Remote '$remoteOpt' not found in $source")
                    return@runBlocking
                }
            }
            fromCurrentFlag -> gitRemoteUrl(Path(workingPath), remoteOpt) ?: run {
                terminal.pDanger("Remote '$remoteOpt' not found in current repository")
                return@runBlocking
            }
            gitUrlOpt != null -> gitUrlOpt!!
            else ->
            {
                terminal.pDanger("Either --git-url, --from-current or --from-path must be specified")
                return@runBlocking
            }
        }

        val cloneUrl = fromPathOpt ?: sourceUrl
        terminal.pMsg("Cloning parent repository '$sourceUrl'")
        gitClone(cloneUrl, parentDir, refNameOpt) { _, _ -> }?.let {
            terminal.pError(it)
            return@runBlocking
        }
        if (fromPathOpt != null) gitSetRemoteUrl(parentDir, remoteOpt, sourceUrl)

        val commit = runCatching { gitHeadCommit(parentDir) }.getOrElse {
            terminal.pDanger("Could not determine HEAD commit of parent repository")
            return@runBlocking
        }
        if (!commit.matches(Regex("[0-9a-fA-F]{40}")))
        {
            terminal.pDanger("Invalid HEAD commit of parent repository: $commit")
            return@runBlocking
        }
        config.parent = ConfigFile.ParentConfig(
            id = sourceUrl,
            version = commit.take(8),
            ref = refNameOpt,
            refType = refTypeOpt?.uppercase()?.let { ConfigFile.RefType.valueOf(it) } ?: gitRefType(parentDir, refNameOpt),
            remoteName = remoteOpt
        )
        updateParentHashes(config)
        config.write()
        addParentToGitignore()

        terminal.pSuccess("Fork initialized")
        terminal.pMsg("Parent: $sourceUrl")
        terminal.pMsg("Commit: ${commit.take(8)}")
    }
}

private class ForkSync : CliktCommand(name = "sync")
{
    override fun help(context: Context) = "Sync the immutable parent checkout"

    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        val parent = config.parent ?: run {
            terminal.pDanger("No parent configured. Run 'pakku fork init' first.")
            return@runBlocking
        }

        if (Dirs.parentDir.notExists())
        {
            terminal.pMsg("Parent repository not found. Cloning...")
            gitClone(parent.id, Dirs.parentDir, parent.ref) { _, _ -> }?.let {
                terminal.pError(it)
                return@runBlocking
            }
        }
        else
        {
            terminal.pMsg("Fetching parent updates")
            val error = when (parent.refType)
            {
                ConfigFile.RefType.BRANCH -> gitUpdate(Dirs.parentDir, parent.ref) { _, _ -> }
                ConfigFile.RefType.TAG, ConfigFile.RefType.COMMIT -> gitFetchCheckout(Dirs.parentDir, parent.ref) { _, _ -> }
            }
            error?.let {
                terminal.pError(it)
                return@runBlocking
            }
        }

        val commit = runCatching { gitHeadCommit(Dirs.parentDir) }.getOrElse {
            terminal.pDanger("Could not determine HEAD commit of parent repository")
            return@runBlocking
        }
        if (!commit.matches(Regex("[0-9a-fA-F]{40}")))
        {
            terminal.pDanger("Invalid HEAD commit of parent repository: $commit")
            return@runBlocking
        }
        config.parent?.version = commit.take(8)
        updateParentHashes(config)
        config.write()

        terminal.pSuccess("Parent sync complete")
        terminal.pMsg("Commit: ${commit.take(8)}")
    }
}

private class ForkShow : CliktCommand(name = "show")
{
    override fun help(context: Context) = "Show fork configuration"

    override fun run()
    {
        val config = ConfigFile.readOrNew()
        val parent = config.parent ?: run {
            terminal.pMsg("No fork configured.")
            return
        }

        terminal.pMsg("Parent URL: ${parent.id}")
        terminal.pMsg("Ref: ${parent.ref} (${parent.refType.name.lowercase()})")
        terminal.pMsg("Remote: ${parent.remoteName}")
        terminal.pMsg("Last synced commit: ${parent.version ?: "never synced"}")
        config.parentLockHash?.let { terminal.pMsg("Parent lock hash: $it") }
        if (config.excludes.isNotEmpty()) terminal.pMsg("Excluded projects: ${config.excludes.joinToString()}")
    }
}

private class ForkUnset : CliktCommand(name = "unset")
{
    override fun help(context: Context) = "Remove fork configuration and parent checkout"

    @OptIn(ExperimentalPathApi::class)
    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        if (config.parent == null)
        {
            terminal.pMsg("No fork configured.")
            return@runBlocking
        }
        if (!terminal.ynPrompt("Do you really want to remove the fork parent?")) return@runBlocking

        if (Dirs.parentDir.exists()) Dirs.parentDir.deleteRecursively()
        config.parent = null
        config.parentLockHash = null
        config.parentConfigHash = null
        config.excludes.clear()
        config.write()
        terminal.pDanger("Fork configuration removed")
    }
}

private class ForkPromote : CliktCommand(name = "promote")
{
    override fun help(context: Context) = "Copy parent projects into the local lock file"
    private val projectsArgs: List<String> by argument("project").multiple(required = true)

    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        if (config.parent == null)
        {
            terminal.pDanger("No parent configured. Run 'pakku fork init' first.")
            return@runBlocking
        }
        val parentLockPath = parentLockFilePath() ?: run {
            terminal.pDanger("Parent lock file not found. Run 'pakku fork sync' first.")
            return@runBlocking
        }
        val parentLock = LockFile.readOrNewFrom(parentLockPath)
        val localLock = LockFile.readOrNew()
        val missing = mutableListOf<String>()

        projectsArgs.forEach { input ->
            val project = parentLock.getProject(input)
            if (project == null) missing += input else localLock.add(project)
        }

        if (missing.isNotEmpty())
        {
            terminal.pDanger("Project(s) not found in parent: ${missing.joinToString()}")
            return@runBlocking
        }

        localLock.write()
        terminal.pSuccess("Promoted ${projectsArgs.size} project(s) to the local lock file")
    }
}

private class ForkExclude : CliktCommand(name = "exclude")
{
    override fun help(context: Context) = "Exclude parent projects from exports"
    private val projectsArgs: List<String> by argument("project").multiple(required = true)

    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        config.excludes.addAll(projectsArgs)
        config.write()
        terminal.pSuccess("Excluded ${projectsArgs.size} parent project(s)")
    }
}

private class ForkInclude : CliktCommand(name = "include")
{
    override fun help(context: Context) = "Re-include excluded parent projects"
    private val projectsArgs: List<String> by argument("project").multiple(required = true)

    override fun run(): Unit = runBlocking {
        val config = ConfigFile.readOrNew()
        config.excludes.removeAll(projectsArgs.toSet())
        config.write()
        terminal.pSuccess("Re-included ${projectsArgs.size} parent project(s)")
    }
}

private fun updateParentHashes(config: ConfigFile)
{
    config.parentLockHash = parentLockFilePath()?.let { sha256(it) }
    config.parentConfigHash = parentConfigFilePath()?.let { sha256(it) }
}

private fun addParentToGitignore()
{
    val gitignore = Path(workingPath, ".gitignore")
    val entry = "${Dirs.PAKKU_DIR}/parent"
    if (gitignore.notExists()) gitignore.createFile()
    val lines = gitignore.readText().lines()
    if (entry !in lines.map { it.trim() })
    {
        val prefix = if (lines.lastOrNull().isNullOrBlank()) "" else "\n"
        gitignore.writeText(gitignore.readText() + prefix + entry + "\n")
    }
}
