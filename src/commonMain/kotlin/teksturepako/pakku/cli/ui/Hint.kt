package teksturepako.pakku.cli.ui

fun hint(vararg text: String): String
{
    return " ".repeat(2) + "(${text.joinToString("")})"
}