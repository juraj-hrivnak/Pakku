package teksturepako.pakku.cli.arg

fun splitProjectArg(arg: String): Pair<String, String?>
{
    val splitArg = arg.split(":")
    return splitArg[0] to splitArg.getOrNull(1)
}
