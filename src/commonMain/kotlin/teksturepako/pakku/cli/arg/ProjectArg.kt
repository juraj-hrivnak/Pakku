package teksturepako.pakku.cli.arg

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.errors.ActionError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class ProjectArg(open val rawArg: String)
{
    data class CommonArg(val input: String, val fileId: String?, override val rawArg: String) : ProjectArg(rawArg)
    data class GitHubArg(val owner: String, val repo: String, val tag: String?, override val rawArg: String) : ProjectArg(rawArg)

    @OptIn(ExperimentalContracts::class)
    inline fun <T> fold(
        commonArg: (CommonArg) -> T,
        gitHubArg: (GitHubArg) -> T
    ): T
    {
        contract {
            callsInPlace(commonArg, InvocationKind.AT_MOST_ONCE)
            callsInPlace(gitHubArg, InvocationKind.AT_MOST_ONCE)
        }

        return when (this)
        {
            is CommonArg -> commonArg(this)
            is GitHubArg -> gitHubArg(this)
        }
    }
}

data class ArgFailed(val arg: String, val argType: String) : ActionError()
{
    override val rawMessage = "Failed to process $argType arg: '$arg'"
}

data class EmptyArg(val argType: String) : ActionError()
{
    override val rawMessage = "$argType arg is empty"
}

fun splitCommonArg(arg: String): Result<ProjectArg.CommonArg, ActionError>
{
    val splitArg = arg.split(":")

    val input = splitArg.getOrNull(0) ?: return Err(EmptyArg("project"))

    return Ok(ProjectArg.CommonArg(input, splitArg.getOrNull(1), rawArg = arg))
}

fun splitGitHubArg(arg: String): Result<ProjectArg.GitHubArg, ActionError>
{
    val splitArg = arg.replace(".*github.com/".toRegex(), "").split("/", "@")

    val owner = splitArg.getOrNull(0) ?: return Err(EmptyArg("GitHub project"))
    val repo = splitArg.getOrNull(1) ?: return Err(ArgFailed(arg, "GitHub project"))

    val tag = when
    {
        "@" in arg ->
        {
            arg.substringAfter("@")
        }
        ".*github.com/".toRegex() in arg && ".*(tag|tree)/".toRegex() in arg ->
        {
            arg.replace(".*(tag|tree)/".toRegex(), "").split("/").getOrNull(0)
        }
        else -> null
    }

    return Ok(ProjectArg.GitHubArg(owner, repo, tag, rawArg = arg))
}

fun mapProjectArg(arg: String): Result<ProjectArg, ActionError>
{
    return if ("/" in arg) splitGitHubArg(arg)
    else splitCommonArg(arg)
}

