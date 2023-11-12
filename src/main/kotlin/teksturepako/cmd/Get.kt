package teksturepako.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import kotlinx.coroutines.runBlocking
import teksturepako.platforms.CurseForge
import teksturepako.platforms.Modrinth

class Get : CliktCommand()
{
    private val mods: List<String> by argument().multiple()

    override fun run() = runBlocking {
        mods.forEach { arg ->
            var cf = CurseForge.requestProject(arg)
            var mr = Modrinth.requestProject(arg)

            when
            {
                cf != null -> echo("CurseForge: $cf")
                mr != null ->
                {
                    cf = CurseForge.requestProjectFromSlug(mr.slug.replace("\"", ""))
                    echo("CurseForge: $cf")
                }
            }

            when
            {
                mr != null -> echo("Modrinth: $mr")
                cf != null ->
                {
                    mr = Modrinth.requestProjectFromSlug(cf.slug.replace("\"", ""))
                    echo("Modrinth: $mr")
                }
            }

            echo()
        }
    }

}