package teksturepako.pakku.cli.ui

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.Theme
import teksturepako.pakku.api.data.allEqual
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.UpdateStrategy
import teksturepako.pakku.api.projects.containProject

fun Project.getFlavoredName(theme: Theme, maxLength: Int? = 20): String?
{
    val name = this.name.values.firstOrNull() ?: return null

    return if (this.redistributable)
    {
        if (this.hasNoFiles()) red(name) else name
    }
    else
    {
        if (this.hasNoFiles())
        {
            TextStyle(bgColor = white, color = red)(theme.string("pakku.warning_sign", "!") + name)
        }
        else
        {
            TextStyle(bgColor = white, color = black)(theme.string("pakku.warning_sign", "!") + name)
        }
    }
}

fun Project.getFlavoredUpdateMsg(theme: Theme, updatedProjects: MutableSet<Project>): String = when (this.updateStrategy)
{
    UpdateStrategy.LATEST      ->
    {
        val symbol = theme.string("pakku.update_strategy.latest", "^")
        if (updatedProjects containProject this) blue(symbol) else brightGreen(symbol)
    }
//    UpdateStrategy.SAME_LATEST -> cyan(theme.string("pakku.update_strategy.same_latest", "*^"))
    UpdateStrategy.NONE        -> red(theme.string("pakku.update_strategy.none", "x^"))
}

fun Project.getFlavoredSlug(): String = buildString {
    append(dim("{"))

    val project = this@getFlavoredSlug
    val slugs = this@getFlavoredSlug.slug

    if (slugs.values.allEqual() && slugs.values.size > 1)
    {
        val providers: List<Provider> = slugs
            .map { (platform, _) -> platform }
            .mapNotNull { provider ->
                Provider.getProvider(provider)
            }

        val resultText = providers
            .joinToString(dim(", "), dim("["), dim("]")) { provider ->
                val coloredProvName = if (project.hasFilesOn(provider)) dim(provider.shortName) else red(provider.shortName)
                if (provider is Platform)
                {
                    val hyperlink = "${provider.getUrlForProjectType(project.type)}/${project.slug[provider.serialName]}"

                    coloredProvName.createHyperlink(hyperlink)
                }
                else
                {
                    provider.siteUrl?.let {
                        coloredProvName.createHyperlink("$it/${slugs[provider.serialName]}")
                    } ?: coloredProvName
                }
            }
            .plusDim("=")
            .plusStrong(slugs.values.first())

        append(resultText)
    }
    else
    {
        val providers: List<Pair<Provider, String>> = slugs
            .mapNotNull { (platform, slug) ->
                val provider = Provider.getProvider(platform) ?: return@mapNotNull null
                provider to slug
            }

        val resultText = providers
            .map { (provider, slug) ->
                val coloredProvName = if (project.hasFilesOn(provider)) dim(provider.shortName) else red(provider.shortName)
                val hyperlinkProvName = if (provider is Platform)
                {
                    val hyperlink = "${provider.getUrlForProjectType(project.type)}/${project.slug[provider.serialName]}"
                    coloredProvName.createHyperlink(hyperlink)
                }
                else
                {
                    provider.siteUrl?.let { coloredProvName.createHyperlink("$it/$slug") } ?: coloredProvName
                }

                hyperlinkProvName to slug
            }
            .joinToString(dim(", ")) { (provShortName, slug) ->
                provShortName.plusDim("=").plusStrong(slug)
            }

        append(resultText)
    }

    append(dim("}"))
}

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
