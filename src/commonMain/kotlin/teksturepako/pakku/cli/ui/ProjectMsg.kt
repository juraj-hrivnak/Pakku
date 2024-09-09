package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Theme
import teksturepako.pakku.api.data.allEqual
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.containsProject

fun Project.getFlavoredName(theme: Theme, maxLength: Int? = null): String?
{
    val name = this.name.values.firstOrNull()?.let {
        if (maxLength != null && it.length > maxLength)
        {
            it.take(maxLength + 1) + "..."
        }
        else it + " ".repeat(3)
    } ?: return null

    return when
    {
        this.redistributable -> if (this.hasNoFiles()) " " + red(name) else " $name"
        else                 ->
        {
            if (this.hasNoFiles())
            {
                TextStyle(bgColor = white, color = red)("${theme.string("pakku.warning_sign", "!")}$name")
            }
            else
            {
                TextStyle(bgColor = white, color = black)("${theme.string("pakku.warning_sign", "!")}$name")
            }
        }
    }
}

fun Project.getFlavoredUpdateMsg(theme: Theme, updatedProjects: MutableSet<Project>): String = when (this.updateStrategy)
{
    UpdateStrategy.LATEST      ->
    {
        val symbol = theme.string("pakku.update_strategy.latest", "^")
        if (updatedProjects containsProject this) blue(symbol) else brightGreen(symbol)
    }
    UpdateStrategy.SAME_LATEST -> cyan(theme.string("pakku.update_strategy.same_latest", "*^"))
    UpdateStrategy.NONE        -> red(theme.string("pakku.update_strategy.none", "x^"))
}

fun Project.getFlavoredSlug(): String = dim("{") + if (this.slug.values.allEqual() && this.slug.values.size > 1)
{
    this.slug
        .map { (platform, _) ->
            Multiplatform.getPlatform(platform)?.let {
                val hyperlink = "${it.getUrlForProjectType(this.type)}/${this.slug[it.serialName]}"

                if (this.hasFilesOnPlatform(it))
                {
                    dim(it.shortName).createHyperlink(hyperlink)
                }
                else
                {
                    red(it.shortName).createHyperlink(hyperlink)
                }
            }
        }
        .joinToString(dim(", "), dim("["), dim("]"))
        .plusDim("=")
        .plusStrong(this.slug.values.first())
}
else
{
    this.slug
        .map { (platform, slug) ->
            Multiplatform.getPlatform(platform)?.let {
                val hyperlink = "${it.getUrlForProjectType(this.type)}/${this.slug[it.serialName]}"

                if (this.hasFilesOnPlatform(it))
                {
                    dim(it.shortName).createHyperlink(hyperlink)
                }
                else
                {
                    red(it.shortName).createHyperlink(hyperlink)
                }
            }?.plusDim("=")?.plusStrong(slug)
        }
        .joinToString(dim(", "))
} + dim("}")

fun Project.getFullMsg(): String = "${dim(this.type)} ${this.getFlavoredSlug()}"

fun Project.getFlavoredTargets(platforms: List<Platform>): String
{
    return platforms.joinToString(" ") {
        if (this.hasFilesOnPlatform(it))
        {
            TextStyle(
                color = brightGreen,
                hyperlink = "${it.getUrlForProjectType(this.type)}/${this.slug[it.serialName]}"
            )(it.shortName)
        }
        else red(it.shortName)
    }
}