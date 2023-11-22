package teksturepako.platforms

import teksturepako.data.finalize
import teksturepako.data.pakkuId
import teksturepako.projects.CfFile
import teksturepako.projects.Project
import teksturepako.projects.ProjectFile

object Multiplatform {
    val platforms = ArrayList<Platform>()

    /**
     * @return a [project][Project] with empty files.
     */
    suspend fun requestProject(input: String): Project? {
        val pakkuId = pakkuId(input)

        var cf = CurseForge.requestProject(input)?.apply { this.pakkuId = pakkuId }
        var mr = Modrinth.requestProject(input)?.apply { this.pakkuId = pakkuId }

        // Retrieve project from another platform if it's missing
        if (cf == null && mr != null) {
            cf = CurseForge.requestProjectFromSlug(mr.slug[Modrinth.serialName]!!.replace("\"", ""))
        } else if (mr == null && cf != null) {
            mr = Modrinth.requestProjectFromSlug(cf.slug[CurseForge.serialName]!!.replace("\"", ""))
        }

        // Combine projects or return one just of them
        return cf?.let { c ->
            mr?.let { m ->
                c + m
            } ?: c
        } ?: mr
    }

    /**
     * @param numberOfFiles The number of requested files for each platform.
     * @return a [project][Project] with [files][ProjectFile] provided.
     */
    suspend fun requestProjectFile(
        mcVersions: List<String>,
        loaders: List<String>,
        input: String,
        numberOfFiles: Int = 1
    ): Project? {
        val project = requestProject(input) ?: return null

        // CurseForge
        project.id[CurseForge.serialName].finalize().let { projectId ->
            CurseForge.requestProjectFilesFromId(mcVersions, loaders, projectId)
                .take(numberOfFiles)
                .filterIsInstance<CfFile>()
                .forEach { file ->
                    // Request URL and add to project files
                    if (file.url != "null") project.files.add(file) else
                    {
                        val url = CurseForge.fetchAlternativeDownloadUrl(file.id, file.fileName)
                        project.files.add(file.apply {
                            // Replace empty character
                            this.url = url.replace(" ", "%20")
                        })
                    }
                }
        }

        // Modrinth
        project.id[Modrinth.serialName].finalize().let { projectId ->
            Modrinth.requestProjectFilesFromId(mcVersions, loaders, projectId)
                .take(numberOfFiles)
                .also { project.files.addAll(it) }
        }

        return project
    }
}