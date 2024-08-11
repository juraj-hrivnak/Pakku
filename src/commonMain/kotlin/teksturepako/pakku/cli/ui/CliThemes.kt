package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.Theme

object CliThemes
{
    /** Default theme for Pakku that extends the Mordant default theme. */
    val Default: Theme = Theme(Theme.Default) {
        strings["pakku.prefix"] = "❯❯❯"
        strings["pakku.warning_sign"] = "⚠"

        strings["pakku.update_strategy.latest"] = "^"
        strings["pakku.update_strategy.same_latest"] = "★^"
        strings["pakku.update_strategy.none"] = "✖^"
    }

    /**
     *  ASCII theme for Pakku that replaces UTF-8 characters
     *  from the Pakku default theme with respective ASCII characters.
     */
    val Ascii: Theme = Theme(this.Default) {
        strings["pakku.prefix"] = ">>>"
        strings["pakku.warning_sign"] = "!"

        strings["pakku.update_strategy.same_latest"] = "*^"
        strings["pakku.update_strategy.none"] = "x^"

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
