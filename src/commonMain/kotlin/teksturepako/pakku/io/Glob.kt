package teksturepako.pakku.io

import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

@OptIn(ExperimentalPathApi::class)
fun Path.listDirectoryEntriesRecursive(glob: String): List<Path>
{
    val matcher = FileSystems.getDefault().getPathMatcher("glob:${glob.removePrefix("./")}")
    return walk(PathWalkOption.FOLLOW_LINKS, PathWalkOption.INCLUDE_DIRECTORIES).filter { matcher.matches(it) }.toList()
}

fun List<String>.expandWithGlob() = flatMap { glob ->
    Path("").listDirectoryEntriesRecursive(glob).map { it.toString() }
}