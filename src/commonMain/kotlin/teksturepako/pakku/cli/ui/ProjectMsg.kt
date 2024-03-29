package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import teksturepako.pakku.api.projects.Project

fun Project.getFlavoredProjectName(): String? = when
{
    this.redistributable ->
    {
        if (this.hasNoFiles()) this.name.values.firstOrNull()?.let { red(it) }
        else this.name.values.firstOrNull()
    }
    else                    ->
    {
        if (this.hasNoFiles()) this.name.values.firstOrNull()?.let {
            TextStyle(bgColor = white, color = red)("⚠$it")
        }
        else this.name.values.firstOrNull()?.let {
            TextStyle(bgColor = white, color = black)("⚠$it")
        }
    }
}

fun dim(string: String): String = TextStyle(color = brightBlue)(string)

fun Project.getFlavoredSlug(): String = "${dim("{")}${this.slug.map { (platform, slug) ->
    "$platform${dim("=")}$slug"
}.joinToString(dim(", "))}${dim("}")}"