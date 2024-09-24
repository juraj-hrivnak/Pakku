package teksturepako.pakku.cli.arg

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import teksturepako.pakku.api.actions.ActionError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class ProjectArg(open val rawArg: String)
{
    data class Arg(val input: String, val fileId: String?, override val rawArg: String) : ProjectArg(rawArg)
    data class GitHubArg(val owner: String, val repo: String, override val rawArg: String) : ProjectArg(rawArg)

    @OptIn(ExperimentalContracts::class)
    inline fun <T> fold(
        arg: (Arg) -> T,
        gitHubArg: (GitHubArg) -> T,
    ): T
    {
        contract {
            callsInPlace(arg, InvocationKind.AT_MOST_ONCE)
            callsInPlace(gitHubArg, InvocationKind.AT_MOST_ONCE)
        }

        return when (this)
        {
            is Arg       -> arg(this)
            is GitHubArg -> gitHubArg(this)
        }
    }
}

data class ArgFailed(val arg: String, val argType: String) :
    ActionError("Failed to process $argType arg: '$arg'")

data class EmptyArg(val argType: String) : ActionError("$argType arg is empty")

fun splitProjectArg(arg: String): Result<ProjectArg.Arg, ActionError>
{
    val splitArg = arg.split(":")
    val input = splitArg.getOrNull(0) ?: return Err(EmptyArg("project"))
    return Ok(ProjectArg.Arg(input, splitArg.getOrNull(1), rawArg = arg))
}

fun splitGitHubProjectArg(arg: String): Result<ProjectArg.GitHubArg, ActionError>
{
    val splitArg = arg.replace(".*github.com/".toRegex(), "").split("/")

    val owner = splitArg.getOrNull(0) ?: return Err(EmptyArg("GitHub project"))
    val repo = splitArg.getOrNull(1) ?: return Err(ArgFailed(arg, "GitHub project"))

    return Ok(ProjectArg.GitHubArg(owner, repo, rawArg = arg))
}

fun mapProjectArg(arg: String): Result<ProjectArg, ActionError>
{
    return if ("/" in arg) splitGitHubProjectArg(arg)
    else splitProjectArg(arg)
}

