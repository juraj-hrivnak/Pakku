package teksturepako.pakku.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import korlibs.io.file.extension
import korlibs.io.file.pathInfo
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.*
import teksturepako.pakku.api.data.PakkuLock
import teksturepako.pakku.api.http.Http
import teksturepako.pakku.api.overrides.Overrides
import teksturepako.pakku.api.overrides.Overrides.PROJECT_OVERRIDES_FOLDER
import teksturepako.pakku.api.platforms.Multiplatform
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType

class Fetch : CliktCommand("Fetch projects to your pack folder")
{
    override fun run() = runBlocking {
        val pakkuLock = PakkuLock.readToResult().getOrElse {
            terminal.danger(it.message)
            echo()
            return@runBlocking
        }

        var fetched = false
        val ignored = mutableMapOf<ProjectType, List<VfsFile>>()
        val jobs = mutableListOf<Job>()

//        // Progress bar
//        val progress = terminal.progressAnimation {
//            text("Fetching ")
//            spinner(Spinner.Dots())
//            percentage()
//            padding = 0
//        }

        var maxSize: Long = 0

        for (project in pakkuLock.getAllProjects())
        {
            var projectFile: ProjectFile? = null

            x@ for (platform in Multiplatform.platforms)
            {
                projectFile = project.getFilesForPlatform(platform)
                    .takeIf { it.isNotEmpty() }
                    ?.first() ?: continue@x
                break@x
            }

            if (projectFile?.url == null)
            {
                terminal.danger("No files found for ${project.slug}")
                echo()
                continue
            }

            val outputFile = localCurrentDirVfs["${project.type.folderName}/${projectFile.fileName}"]
                .also { ignored[project.type] = ignored[project.type]?.let { list -> list + it } ?: listOf(it) }

            val parentFolder = localCurrentDirVfs[project.type.folderName]

            // Skip to next if output file exists
            if (outputFile.exists()) continue

            maxSize += projectFile.size

            // Download
            val deferred = async {
                Http().requestByteArray(projectFile.url!!) { _, _ ->
//                    progress.updateTotal(maxSize)
                } to projectFile.size
            }

            jobs += launch(Dispatchers.IO) {
                // Create parent folders
                if (!parentFolder.exists()) parentFolder.mkdirs()

                val (bytes, fileSize) = deferred.await()

                if (bytes != null) try
                {
                    // Write to file
                    outputFile.writeBytes(bytes)
                    terminal.success("${outputFile.baseName} saved")
//                    progress.advance(fileSize.toLong())

                    fetched = true
                } catch (e: Exception)
                {
                    e.printStackTrace()
                }
            }
        }

        jobs.joinAll()

        if (fetched)
        {
//            progress.clear()
            terminal.success("Projects successfully fetched")
            echo()
        }

        // -- OVERRIDES --

        val projectOverrides = Overrides.getProjectOverrides()
        var synced = false

        projectOverrides.map { projectOverride ->
            launch {
                val file = localCurrentDirVfs["${projectOverride.type.folderName}/${projectOverride.fileName}"]
                if (!file.exists()) runCatching {
                    file.parent.mkdir()
                    localCurrentDirVfs["$PROJECT_OVERRIDES_FOLDER/${projectOverride.type.folderName}/" +
                            projectOverride.fileName].copyTo(file)

                    terminal.info("${projectOverride.fileName} synced")
                    synced = true
                }
            }
        }.joinAll()

        if (synced)
        {
            terminal.info("Project overrides successfully synced")
            echo()
        }

        // -- OLD FILES --

        var removed = false

        val ignoredProjectOverrides = projectOverrides.map { projectOverride ->
            projectOverride.fileName
        }

        ignored.map { (projectType, ignoredFiles) ->
            launch(Dispatchers.IO) {
                val ignoredNames = ignoredFiles.map { it.baseName }

                localCurrentDirVfs[projectType.folderName].listSimple()
                    .filter { file ->
                        file.baseName !in ignoredNames
                                && file.baseName !in ignoredProjectOverrides
                                && file.extension in listOf("jar", "zip")
                                && file.parent.baseName == projectType.folderName
                    }.forEach { file ->
                        file.delete()
                        terminal.warning("${file.baseName} deleted")
                        removed = true
                    }
            }
        }.joinAll()

        if (removed)
        {
            terminal.warning("Old project files successfully deleted")
            echo()
        }
    }
}