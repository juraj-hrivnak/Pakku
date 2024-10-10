package teksturepako.pakku.io

import teksturepako.pakku.debug
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.PathWalkOption
import kotlin.io.path.walk

@OptIn(ExperimentalPathApi::class)
fun Path.listDirectoryEntriesRecursive(glob: String): Sequence<Path>
{
    val matcher = FileSystems.getDefault().getPathMatcher("glob:${glob.removePrefix("./")}")
    return walk(PathWalkOption.INCLUDE_DIRECTORIES).filter { matcher.matches(it) }
}

fun List<String>.expandWithGlob() = fold(listOf<String>()) { acc, glob ->
    if (glob.startsWith("!"))
    {
        acc - Path("").listDirectoryEntriesRecursive(glob.removePrefix("!")).map { it.toString() }.toSet()
    }
    else
    {
        acc + Path("").listDirectoryEntriesRecursive(glob).map { it.toString() }
    }
}.debug(::println)
