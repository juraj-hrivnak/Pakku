package teksturepako.pakku.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.debugIfNotEmpty
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.*

@JvmInline
value class Glob private constructor(private val value: Pair<PathMatcher, Boolean>)
{
    val pathMatcher: PathMatcher get() = value.first
    val isNegated: Boolean get() = value.second

    constructor(pathMatcher: PathMatcher, isNegated: Boolean) : this(pathMatcher to isNegated)
}

@OptIn(ExperimentalPathApi::class)
suspend fun Path.walk(
    globPatterns: List<String>,
): Sequence<Pair<Path, Boolean>> = withContext(Dispatchers.IO)
{
    val matchers = globPatterns.map { pattern ->
        val isNegated = pattern.startsWith("!")
        val cleanPattern = pattern.removePrefix("!").removePrefix("./")
        val finalPattern = if (cleanPattern.endsWith("/")) cleanPattern.dropLast(1) else cleanPattern

        val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$finalPattern")

        Glob(pathMatcher, isNegated)
    }

    val walk = walk(PathWalkOption.INCLUDE_DIRECTORIES)

    walk
        .mapNotNull { path ->
            val relativePath = path.relativeTo(this@walk)

            matchers
                .findLast { glob ->
                    glob.pathMatcher.matches(relativePath)
                }
                ?.let { it to relativePath }
        }
        .flatMap { (glob, path) ->
            if (path.isDirectory())
            {
                walk
                    .filter { recursivePath ->
                        val relativeRecursivePath = recursivePath.relativeTo(this@walk)
                        val relativeDirPath = path.relativeTo(this@walk)

                        relativeRecursivePath.startsWith(relativeDirPath)
                                && relativeRecursivePath != relativeDirPath
                                && relativeRecursivePath.isRegularFile()
                    }
                    .map { recursivePath ->
                        recursivePath.relativeTo(this@walk) to glob.isNegated
                    }
            }
            else
            {
                sequenceOf(path to glob.isNegated)
            }
        }
}

suspend fun List<String>.expandWithGlob(inputPath: Path): List<String>
{
    val walk = inputPath.walk(this)

    return walk.fold(mutableSetOf<String>()) { acc, (path, isNegated) ->
        when
        {
            isNegated ->
            {
                if (path.isDirectory())
                {
                    acc.removeAll { existingPath ->
                        Path(existingPath).startsWith(path)
                    }
                }
                else
                {
                    acc -= (path.toString())
                }
            }
            else ->
            {
                acc += path.toString()
            }
        }
        acc
    }
        .toList()
        .debugIfNotEmpty { println("expandWithGlob = $it") }
}
