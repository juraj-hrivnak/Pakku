package teksturepako.platforms

import teksturepako.data.finalize
import teksturepako.projects.Project
import teksturepako.projects.ProjectFile

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
        }

        when
        {
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

        if (project.id[CurseForge.name] != null)
        {
            project.files[CurseForge.name] =
                CurseForge.requestProjectFilesFromId(
                    mcVersion,
                    loader,
                    project.id[CurseForge.name].finalize())!!.second.take(3)
        }
        if (project.id[Modrinth.name] != null)
        {
            project.files[Modrinth.name] =
                Modrinth.requestProjectFilesFromId(
                    mcVersion,
                    loader,
                    project.id[Modrinth.name].finalize())!!.second.take(3)
        }
        return project
    }
}