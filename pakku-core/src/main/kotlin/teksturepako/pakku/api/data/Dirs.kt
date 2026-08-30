package teksturepako.pakku.api.data

import kotlin.io.path.Path

object Dirs
{
    const val PAKKU_DIR = ".pakku"
    val cacheDir get() = Path(workingPath, "build", ".cache")
    val shelfDir get() = Path(workingPath, PAKKU_DIR, "shelf")
    val remoteDir get() = Path(workingPath, PAKKU_DIR, "remote")
    val parentDir get() = Path(workingPath, PAKKU_DIR, "parent")
}
