package teksturepako.platforms

import teksturepako.data.finalize
import teksturepako.projects.CfFile
import teksturepako.projects.Project

object Multiplatform
{
    val platforms = ArrayList<Platform>()

    suspend fun requestProject(input: String): Project?
    {
        var cf = CurseForge.requestProject(input)
        var mr = Modrinth.requestProject(input)

        when
        {
            cf == null && mr != null ->
            {
                cf = CurseForge.requestProjectFromSlug(mr.slug.replace("\"", ""))
            }
            mr == null && cf != null ->
            {
                mr = Modrinth.requestProjectFromSlug(cf.slug.replace("\"", ""))
            }
        }

        return when
        {
            cf != null && mr != null ->
            {
                Project(
                    name = (cf.name + mr.name).toMutableMap(),
                    slug = mr.slug,
                    type = mr.type,
                    id = (cf.id + mr.id).toMutableMap(),
                    files = mutableMapOf(),
                )
            }
            cf != null -> cf
            mr != null -> mr
            else -> null
        }
    }

    suspend fun requestProjectFile(mcVersion: String, loader: String, input: String): Project?
    {
        val project = requestProject(input) ?: return null

        // Cf
        if (project.id[CurseForge.serialName] != null)
        {
            val id = project.id[CurseForge.serialName].finalize()
            val rqsts = CurseForge.requestProjectFilesFromId(mcVersion, loader, id)!!.second.take(1)

            rqsts.map { rqst ->
                if (rqst.data is CfFile)
                {
                    val url = CurseForge.requestUrl(id.toInt(), rqst.data.id)

                    if (url != null)
                    {
                        // Replace empty character
                        rqst.url = if (url.contains(" ")) url.replace(" ", "%20") else url
                    }
                }

            }

            project.files[CurseForge.serialName] = rqsts
        }

        // Mr
        if (project.id[Modrinth.serialName] != null)
        {
            val id = project.id[Modrinth.serialName].finalize()
            val rqsts = Modrinth.requestProjectFilesFromId(mcVersion, loader, id)!!.second.take(1)

            project.files[Modrinth.serialName] = rqsts
        }
        return project
    }
}