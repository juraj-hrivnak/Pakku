package teksturepako.pakku.io

import teksturepako.pakku.debugIf
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
fun Path.listDirectoryEntriesRecursive(glob: String): Sequence<Path>
{
    val globPattern = "glob:${this.invariantSeparatorsPathString}/${glob.removePrefix("./")}"
    val matcher = FileSystems.getDefault().getPathMatcher(globPattern)

    return this.walk(PathWalkOption.INCLUDE_DIRECTORIES)
        .filter { matcher.matches(it) }
        .map { Path(it.absolutePathString().removePrefix(this.absolutePathString()).removePrefix(File.separator)) }
}

fun List<String>.expandWithGlob(inputPath: Path) = fold(listOf<String>()) { acc, glob ->
    if (glob.startsWith("!"))
    {
        acc - inputPath.listDirectoryEntriesRecursive(glob.removePrefix("!")).map { it.toString() }.toSet()
    }
    else
    {
        acc + inputPath.listDirectoryEntriesRecursive(glob).map { it.toString() }
    }
}.debugIf({ it.isNotEmpty() }) { println("expandWithGlob = $it") }
