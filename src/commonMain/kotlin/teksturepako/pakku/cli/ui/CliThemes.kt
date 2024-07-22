package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.Theme

object CliThemes
{
    val Default: Theme = Theme(Theme.Default) {
        strings["pakku.prefix"] = "❯❯❯"
    }

    val Ascii: Theme = Theme(this.Default) {
        strings["pakku.prefix"] = ">>>"

        strings["list.number.separator"] = "."
        strings["list.bullet.text"] = "*"
        strings["progressbar.pending"] = " "
        strings["progressbar.complete"] = "#"
        strings["progressbar.separator"] = ">"

        strings["markdown.task.checked"] = "[x]"
        strings["markdown.task.unchecked"] = "[ ]"
        strings["markdown.h1.rule"] = "="
        strings["markdown.h2.rule"] = "-"
        strings["markdown.blockquote.bar"] = "|"

        flags["markdown.table.ascii"] = true
    }
}
