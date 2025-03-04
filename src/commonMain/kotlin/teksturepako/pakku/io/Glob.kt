package teksturepako.pakku.io

import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.debug
import teksturepako.pakku.debugIf
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
suspend fun Path.walk(globPatterns: List<String>): Sequence<Pair<Path, Boolean>> = withContext(Dispatchers.IO) {
    val matchers: List<Pair<PathMatcher, Boolean>> = globPatterns.mapNotNull { input ->
        val negating = input.startsWith("!")
        val glob = (if (negating) input.removePrefix("!") else input).removePrefix("./")

        val globPattern = "glob:${this@walk.invariantSeparatorsPathString}/$glob"
        runCatching { FileSystems.getDefault().getPathMatcher(globPattern) to negating }.get()
    }

    return@withContext this@walk.walk(PathWalkOption.INCLUDE_DIRECTORIES)
        .mapNotNull { path ->
            val matcher = matchers.filter { (matcher) -> matcher.matches(path) }

            if (matcher.isEmpty()) return@mapNotNull null

            matcher to path
        }.map { (matchers, path) ->
            val resultPath = Path(path.absolutePathString().removePrefix(this@walk.absolutePathString()).removePrefix(File.separator))
            val negating = matchers.any { it.second }

            resultPath to negating
        }
}

suspend fun List<String>.expandWithGlob(inputPath: Path): List<String>
{
    val walk = inputPath.walk(this).toList().debug {
        for ((path, negating) in it)
        {
            println("${if (negating) "!" else ""}$path")
        }
    }

    val paths = walk.fold(mutableSetOf<String>()) { acc, (path, negating) ->
        if (negating)
        {
            acc -= path.toString()
        }
        else
        {
            acc += path.toString()
        }

        acc
    }

    return paths.toList().debugIf({ it.isNotEmpty() }) { println("expandWithGlob = $it") }
}

