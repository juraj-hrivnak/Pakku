package teksturepako.pakku.api.data

import teksturepako.pakku.io.tryOrNull
import java.nio.file.Path
import kotlin.io.path.*

var workingPath: String = "."

suspend fun searchUpForWorkingPath(): String?
{
    tailrec suspend fun lookForLockFile(inputPath: Path): Path?
    {
        val lockFile = Path(inputPath.pathString, LockFile.FILE_NAME)

        return if (lockFile.tryOrNull { exists() } == true)
        {
            inputPath
        }
        else
        {
            lookForLockFile(inputPath.parent ?: return null)
        }
    }

    val startingPath = Path(workingPath).tryOrNull { absolute() } ?: return null

    return lookForLockFile(startingPath)?.tryOrNull { absolutePathString() }
}
