package teksturepako.pakku.cli.arg

fun splitProjectArg(arg: String): Pair<String, String?>
{
    val splitArg = arg.split(":")
    return splitArg[0] to splitArg.getOrNull(1)
}

fun splitGitHubProjectArg(arg: String): Pair<String, String>?
{
    val splitArg = arg.replace(".*github.com/".toRegex(), "").split("/")

    val owner = splitArg.getOrNull(0) ?: return null
    val repo = splitArg.getOrNull(1) ?: return null

    return owner to repo
}


