package teksturepako.pakku.api.data

import kotlin.io.path.Path

object Dirs
{
    const val PAKKU_DIR = ".pakku"
    val cacheDir = Path(workingPath, "build", ".cache")
    val shelfDir = Path(workingPath, PAKKU_DIR, "shelf")
}
