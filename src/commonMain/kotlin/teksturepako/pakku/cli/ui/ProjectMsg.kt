package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import teksturepako.pakku.api.data.allEqual
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.containsProject

fun Project.getFlavoredName(maxLength: Int? = null): String?
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
                TextStyle(bgColor = white, color = red)("⚠$name")
            }
            else
            {
                TextStyle(bgColor = white, color = black)("⚠$name")
            }
        }
    }
}

fun Project.getFlavoredUpdateMsg(updatedProjects: MutableSet<Project>): String = when (this.updateStrategy)
{
    UpdateStrategy.LATEST ->
    {
        if (updatedProjects containsProject this)
        {
            blue(this.updateStrategy.short)
        }
        else brightGreen(this.updateStrategy.short)
    }

    UpdateStrategy.NONE   -> red(this.updateStrategy.short)
    else                  -> cyan(this.updateStrategy.short)
}

fun Project.getFlavoredSlug(): String
{
    return dim("{") + if (this.slug.values.allEqual() && this.slug.values.size > 1)
    {
        this.slug
            .map { (platform, _) ->
                Multiplatform.getPlatform(platform)?.let {
                    val hyperlink = "${it.getUrlForProjectType(this.type)}/${this.slug[it.serialName]}"
                    dim(it.shortName).createHyperlink(hyperlink)
                }
            }
            .joinToString(dim(", "), dim("["), dim("]"))
            .addDim("=")
            .addStrong(this.slug.values.first())
    }
    else
    {
        this.slug
            .map { (platform, slug) ->
                Multiplatform.getPlatform(platform)?.let {
                    val hyperlink = "${it.getUrlForProjectType(this.type)}/${this.slug[it.serialName]}"
                    dim(it.shortName).createHyperlink(hyperlink)
                }
                    ?.addDim("=")
                    ?.addStrong(slug)
            }
            .joinToString(dim(", "))
    } + dim("}")
}

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